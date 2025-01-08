package dev.jvmname.acquisitive.ui.screen.commentlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import kotlinx.parcelize.Parcelize
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

@[Composable CircuitInject(CommentListScreen::class, AppScope::class)]
fun CommentListContent(state: CommentListScreen.CommentListState, modifier: Modifier){
    TODO()
}