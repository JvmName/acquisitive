package dev.jvmname.acquisitive.ui.screen.comments

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommentListScreen(val parentItemId: ItemId, val fetchMode: FetchMode) : Screen {
    @Immutable
    data class CommentListState(
        val isRefreshing: Boolean,
        val storyItem: HnScreenItem.Story?,
        val commentItems: List<HnScreenItem.Comment>,
        val eventSink: (CommentListEvent) -> Unit,
    ) : CircuitUiState
}

sealed class CommentListEvent : CircuitUiEvent {
    data object Refresh : CommentListEvent()

    @Poko
    class ExpandToggled(val id: ItemId) : CommentListEvent()

    @Poko
    class Share(val id: ItemId) : CommentListEvent()

    data object StoryClicked : CommentListEvent()
}