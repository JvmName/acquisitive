package dev.jvmname.acquisitive.ui.screen.comments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.jvmname.acquisitive.repo.HnItemRepository
import dev.jvmname.acquisitive.util.rememberRetainedCoroutineScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject


@Inject
class CommentListPresenter(
    private val repo: HnItemRepository,
    @Assisted private val screen: CommentListScreen,
    @Assisted private val navigator: Navigator,
) : Presenter<CommentListScreen.CommentListState> {

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(screen: CommentListScreen, navigator: Navigator): CommentListPresenter
    }

    @Composable
    override fun present(): CommentListScreen.CommentListState {
        val presenterScope = rememberRetainedCoroutineScope()
        var isRefreshing by remember { mutableStateOf(false) }
        return TODO()


    }
}