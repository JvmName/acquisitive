package dev.jvmname.acquisitive.ui.screen.commentlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.repo.HnItemRepository
import dev.jvmname.acquisitive.util.rememberRetainedCoroutineScope
import kotlinx.parcelize.Parcelize
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

@Parcelize
data class CommentListScreen(val parentItemId: ItemId) : Screen {
    data class CommentListState(
        val parentItemId: ItemId,
        val eventSink: (CommentListEvent) -> Unit,
    ) : CircuitUiState
}

sealed class CommentListEvent : CircuitUiEvent {

}

@[Inject CircuitInject(CommentListScreen::class, AppScope::class)]
class CommentListPresenter(
    private val repo: HnItemRepository,
    @Assisted private val screen: CommentListScreen,
    @Assisted private val navigator: Navigator,
) : Presenter<CommentListScreen.CommentListState> {
    @Composable
    override fun present(): CommentListScreen.CommentListState {
        val presenterScope = rememberRetainedCoroutineScope()
        var isRefreshing by remember { mutableStateOf(false) }
        return TODO()


    }
}
