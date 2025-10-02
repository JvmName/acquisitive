package dev.jvmname.acquisitive.ui.screen.main

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
import com.theapache64.rebugger.Rebugger
import dev.jvmname.acquisitive.domain.IntentCreator
import dev.jvmname.acquisitive.domain.StoryScreenItemConverter
import dev.jvmname.acquisitive.network.model.url
import dev.jvmname.acquisitive.repo.story.StoryPagerFactory
import dev.jvmname.acquisitive.repo.story.StoryRepository
import dev.jvmname.acquisitive.ui.screen.comments.CommentListScreen
import dev.jvmname.acquisitive.ui.screen.main.StoryListEvent.CommentsClick
import dev.jvmname.acquisitive.ui.screen.main.StoryListEvent.FavoriteClick
import dev.jvmname.acquisitive.ui.screen.main.StoryListEvent.FetchModeChanged
import dev.jvmname.acquisitive.ui.screen.main.StoryListEvent.Refresh
import dev.jvmname.acquisitive.ui.screen.main.StoryListEvent.StoryClicked
import dev.jvmname.acquisitive.util.rememberRetainedCoroutineScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import logcat.logcat
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_WINDOW = 24

@Suppress("NOTHING_TO_INLINE")
@AssistedInject
class StoryListPresenter(
    private val pagingFactory: StoryPagerFactory,
    private val repo: StoryRepository,
    private val converter: StoryScreenItemConverter,
    private val intentCreator: IntentCreator,
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
                    data.map { ranked -> converter(ranked, fetchMode) }
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
                    val url = repo.getItem(fetchMode, event.id).url ?: return@launch
                    navigator.goTo(intentCreator.view(url))
                }
            }
        }
    }
}