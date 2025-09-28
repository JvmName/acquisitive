package dev.jvmname.acquisitive.ui.screen.comments

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.collectAsRetainedState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.jvmname.acquisitive.domain.CommentScreenItemConverter
import dev.jvmname.acquisitive.domain.IntentCreator
import dev.jvmname.acquisitive.domain.StoryScreenItemConverter
import dev.jvmname.acquisitive.repo.comment.CommentRepository
import dev.jvmname.acquisitive.repo.story.RankedStory
import dev.jvmname.acquisitive.repo.story.StoryRepository
import dev.jvmname.acquisitive.ui.theme.indent
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import dev.jvmname.acquisitive.util.mapList
import dev.jvmname.acquisitive.util.rememberRetainedCoroutineScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Inject
class CommentListPresenter(
    private val repo: CommentRepository,
    private val storyRepo: StoryRepository,
    private val storyConverter: StoryScreenItemConverter,
    private val commentConverter: CommentScreenItemConverter,
    private val intentCreator: IntentCreator,
    @Assisted private val screen: CommentListScreen,
    @Assisted private val navigator: Navigator,
) : Presenter<CommentListScreen.CommentListState> {

    @[AssistedFactory CircuitInject(CommentListScreen::class, AppScope::class)]
    fun interface Factory {
        operator fun invoke(screen: CommentListScreen, navigator: Navigator): CommentListPresenter
    }

    @Composable
    override fun present(): CommentListScreen.CommentListState {
        val presenterScope = rememberRetainedCoroutineScope()
        var isRefreshing by remember { mutableStateOf(true) }
        val isDark = isSystemInDarkTheme()
        val colorScheme = MaterialTheme.colorScheme

        val storyItem by produceState<HnScreenItem.Story?>(null, screen.parentItemId) {
            val story = storyRepo.getItem(screen.fetchMode, screen.parentItemId)
            value = storyConverter(RankedStory(story, 1), screen.fetchMode)
        }

        val comments by remember(screen.parentItemId) {
            repo.observeComments(screen.parentItemId)
                .mapList {
                    commentConverter(
                        it,
                        { colorScheme.indent(it, isDark) }
                    )
                }
        }.collectAsRetainedState(emptyList(), Dispatchers.IO)

        if (isRefreshing) {
            LaunchedEffect(screen.parentItemId) {
                presenterScope.launch(Dispatchers.IO) {
                    repo.refresh(screen.parentItemId)
                    isRefreshing = false
                }
            }
        }
        return CommentListScreen.CommentListState.Full(
            isRefreshing = isRefreshing,
            storyItem = storyItem ?: return CommentListScreen.CommentListState.Loading,
            commentItems = comments,
            eventSink = { event ->
                when (event) {
                    is CommentListEvent.ExpandToggled -> presenterScope.launch {
                        repo.toggleCommentExpanded(event.id)
                    }

                    CommentListEvent.Refresh -> isRefreshing = true
                    is CommentListEvent.Share -> presenterScope.launch {
                        val intent = intentCreator.share(
                            "Check out this story!",
                            "https://news.ycombinator.com/item?id=${event.id}"
                        )
                        navigator.goTo(intent)
                    }
                }
            }
        )
    }
}