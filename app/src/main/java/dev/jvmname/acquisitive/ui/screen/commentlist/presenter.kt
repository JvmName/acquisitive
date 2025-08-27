package dev.jvmname.acquisitive.ui.screen.commentlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.repo.HnItemRepository
import dev.jvmname.acquisitive.util.rememberRetainedCoroutineScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommentListScreen(val parentItemId: ItemId) : Screen {
    data class CommentListState(
        val parentItemId: ItemId,
        val eventSink: (CommentListEvent) -> Unit,
    ) : CircuitUiState
}

sealed class CommentListEvent : CircuitUiEvent {

}

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