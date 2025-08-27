package dev.jvmname.acquisitive.ui.screen.mainlist

import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.map
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuitx.android.IntentScreen
import com.theapache64.rebugger.Rebugger
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.score
import dev.jvmname.acquisitive.repo.HnItemPagerFactory
import dev.jvmname.acquisitive.repo.HnItemRepository
import dev.jvmname.acquisitive.ui.screen.commentlist.CommentListScreen
import dev.jvmname.acquisitive.ui.screen.mainlist.MainListEvent.AddComment
import dev.jvmname.acquisitive.ui.screen.mainlist.MainListEvent.CommentsClick
import dev.jvmname.acquisitive.ui.screen.mainlist.MainListEvent.FavoriteClick
import dev.jvmname.acquisitive.ui.screen.mainlist.MainListEvent.FetchModeChanged
import dev.jvmname.acquisitive.ui.screen.mainlist.MainListEvent.ItemClicked
import dev.jvmname.acquisitive.ui.screen.mainlist.MainListEvent.Refresh
import dev.jvmname.acquisitive.ui.screen.mainlist.MainListEvent.UpvoteClick
import dev.jvmname.acquisitive.ui.types.UrlAndHost
import dev.jvmname.acquisitive.ui.types.toScreenItem
import dev.jvmname.acquisitive.util.rememberRetainedCoroutineScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.toDateTimePeriod
import logcat.LogPriority
import logcat.logcat
import kotlin.time.Duration.Companion.seconds

@Suppress("NOTHING_TO_INLINE")
@Inject
class MainScreenPresenter(
    private val pagingFactory: HnItemPagerFactory,
    private val repo: HnItemRepository,
    @Assisted private val screen: MainListScreen,
    @Assisted private val navigator: Navigator,
) : Presenter<MainListScreen.MainListState> {

    @[AssistedFactory CircuitInject(MainListScreen::class, AppScope::class)]
    fun interface Factory {
        fun create(screen: MainListScreen, navigator: Navigator): MainScreenPresenter
    }

    @Composable
    override fun present(): MainListScreen.MainListState {
        var fetchMode by rememberRetained { mutableStateOf(screen.fetchMode) }
        val presenterScope = rememberRetainedCoroutineScope()
        var isRefreshing by remember { mutableStateOf(false) }
//        LaunchedEffect(fetchMode) {
//            repo.stream(fetchMode)
//                .collectLatest {
//                    println("New items from stream: ${it.size}")
//                }
//        }

        val lazyPaged = rememberRetained(fetchMode) {
            pagingFactory(fetchMode)
                .flow
                .mapLatest { data ->
                    data.map { (item, rank) ->
                        item.toScreenItem(
                            isHot = item.score >= fetchMode.hotThreshold,
                            icon = item.prefixIcon(),
                            rank = rank,
                            time = (kotlin.time.Clock.System.now() - item.time).toDateTimePeriod()
                                .toAbbreviatedDuration(),
                            urlHost = when (item) {
                                is HnItem.Job -> item.url?.let(::extractUrlHost)
                                is HnItem.Story -> item.url?.let(::extractUrlHost)
                                else -> null
                            },
                        )
                    }
                }
                .cachedIn(presenterScope)
        }.collectAsLazyPagingItems(Dispatchers.IO)

        if (isRefreshing) {
            LaunchedEffect(fetchMode) {
                presenterScope.launch(Dispatchers.IO) {
                    lazyPaged.refresh()
                    withTimeoutOrNull(3.seconds) { repo.stream(fetchMode).first() }
                    isRefreshing = false
                }
            }
        }

        Rebugger(
            trackMap = mapOf(
                "fetchMode" to fetchMode,
                "isRefreshing" to isRefreshing,
                "presenterScope" to presenterScope,
                "lazyPaged" to lazyPaged.itemSnapshotList.items.joinToString { it.id.id.toString() },
            ),
            logger = { tag, msg -> logcat(LogPriority.WARN) { "$tag: $msg" } }
        )
        return MainListScreen.MainListState(
            isRefreshing = isRefreshing,
            fetchMode = fetchMode,
            pagedStories = lazyPaged,
        ) { event ->
            when (event) {
                is FetchModeChanged -> {
                    fetchMode = event.fetchMode
                }

                is ItemClicked -> {
                    navigator.goTo(IntentScreen(Intent(Intent.ACTION_VIEW, Uri.parse(event.url))))
                }

                is CommentsClick -> {
                    navigator.goTo(CommentListScreen(event.id))
                }

                AddComment -> navigator
                FavoriteClick -> TODO()
                UpvoteClick -> TODO()
                Refresh -> isRefreshing = true
            }
        }
    }

    private fun HnItem.prefixIcon() = when {
        dead == true -> "☠️"
        deleted == true -> "🗑️"
        this is HnItem.Job -> "💼"
        this is HnItem.Poll -> "🗳️"
        else -> null
    }


    companion object {
        private const val DEFAULT_WINDOW = 24

        private const val HOT_THRESHOLD_HIGH = 900
        private const val HOT_THRESHOLD_NORMAL = 300
        private const val HOT_THRESHOLD_LOW = 30

        @VisibleForTesting
        internal val FetchMode.hotThreshold: Int
            get() = when (this) {
                FetchMode.BEST -> HOT_THRESHOLD_HIGH
                FetchMode.NEW -> HOT_THRESHOLD_LOW
                else -> HOT_THRESHOLD_NORMAL
            }

        @VisibleForTesting
        internal fun DateTimePeriod.toAbbreviatedDuration(): String = when {
            years > 0 -> "${years}y"
            months > 0 -> "${months}mo"
            days >= 7 -> "${days / 7}w"
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            seconds > 10 -> "${seconds}s"
            else -> "<1s"
        }

        @VisibleForTesting
        internal inline fun extractUrlHost(url: String): UrlAndHost = try {
            val host = if (url.isBlank()) ""
            else Uri.parse(url).host
                .orEmpty()
                .removePrefix("www.")
            UrlAndHost(url, host)
        } catch (e: Exception) {
            UrlAndHost(url, "")
        }
    }
}