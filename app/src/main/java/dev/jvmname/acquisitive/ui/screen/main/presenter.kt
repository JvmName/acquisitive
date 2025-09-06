package dev.jvmname.acquisitive.ui.screen.main

import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
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
import dev.jvmname.acquisitive.network.model.url
import dev.jvmname.acquisitive.repo.story.StoryPagerFactory
import dev.jvmname.acquisitive.repo.story.StoryRepository
import dev.jvmname.acquisitive.ui.screen.comments.CommentListScreen
import dev.jvmname.acquisitive.ui.screen.main.StoryListEvent.CommentsClick
import dev.jvmname.acquisitive.ui.screen.main.StoryListEvent.FavoriteClick
import dev.jvmname.acquisitive.ui.screen.main.StoryListEvent.FetchModeChanged
import dev.jvmname.acquisitive.ui.screen.main.StoryListEvent.Refresh
import dev.jvmname.acquisitive.ui.screen.main.StoryListEvent.StoryClicked
import dev.jvmname.acquisitive.ui.types.toScreenItem
import dev.jvmname.acquisitive.util.rememberRetainedCoroutineScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import logcat.LogPriority
import logcat.logcat
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@Suppress("NOTHING_TO_INLINE")
@Inject
class StoryListPresenter(
    private val pagingFactory: StoryPagerFactory,
    private val repo: StoryRepository,
    @Assisted private val screen: StoryListScreen,
    @Assisted private val navigator: Navigator,
) : Presenter<StoryListScreen.StoryListState> {

    @[AssistedFactory CircuitInject(StoryListScreen::class, AppScope::class)]
    fun interface Factory {
        fun create(screen: StoryListScreen, navigator: Navigator): StoryListPresenter
    }

    @Composable
    override fun present(): StoryListScreen.StoryListState {
        var fetchMode by rememberRetained { mutableStateOf(screen.fetchMode) }
        val presenterScope = rememberRetainedCoroutineScope()
        var isRefreshing by remember { mutableStateOf(false) }

        val lazyPaged = rememberRetained(fetchMode) {
            pagingFactory(fetchMode)
                .flow
                .mapLatest { data ->
                    data.map { ranked ->
                        val story = ranked.item
                        story.toScreenItem(
                            isHot = story.score >= fetchMode.hotThreshold,
                            suffixIcon = story.prefixIcon(),
                            rank = ranked.rank,
                            time =story.time.periodUntil(Clock.System.now(), TimeZone.currentSystemDefault())
                                .toAbbreviatedDuration(),
                            urlHost = story.url?.let(::extractUrlHost)
                        )
                    }
                }
                .cachedIn(presenterScope)
        }.collectAsLazyPagingItems(Dispatchers.IO)

        if (isRefreshing) {
            LaunchedEffect(fetchMode) {
                presenterScope.launch(Dispatchers.IO) {
                    lazyPaged.refresh()
                    withTimeoutOrNull(3.seconds) { repo.refresh(fetchMode, DEFAULT_WINDOW) }
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
        return StoryListScreen.StoryListState(
            isRefreshing = isRefreshing,
            fetchMode = fetchMode,
            pagedStories = lazyPaged,
        ) { event ->
            when (event) {
                is FetchModeChanged -> fetchMode = event.fetchMode
                is CommentsClick -> navigator.goTo(CommentListScreen(event.id, fetchMode))
                FavoriteClick -> TODO()
                Refresh -> isRefreshing = true
                is StoryClicked -> presenterScope.launch {
                    val url = repo.getItem(fetchMode, event.id).url?.toUri() ?: return@launch
                    navigator.goTo(IntentScreen(Intent(Intent.ACTION_VIEW, url)))
                }
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
        internal inline fun extractUrlHost(url: String): String = try {
            if (url.isBlank()) ""
            else url.toUri()
                .host
                .orEmpty()
                .removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
    }
}