package dev.jvmname.acquisitive.ui.screen.comments

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dev.jvmname.acquisitive.network.model.ItemId
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