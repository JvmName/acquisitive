package dev.jvmname.acquisitive.ui.screen.main

import androidx.paging.compose.LazyPagingItems
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class StoryListScreen(val fetchMode: FetchMode = FetchMode.TOP) : Screen {
    data class StoryListState(
        val isRefreshing: Boolean,
        val fetchMode: FetchMode,
        val pagedStories: LazyPagingItems<HnScreenItem.Story>,
        val eventSink: (StoryListEvent) -> Unit,
    ) : CircuitUiState
}

sealed class StoryListEvent : CircuitUiEvent {
    @Poko
    class FetchModeChanged(val fetchMode: FetchMode) : StoryListEvent()

    @Poko
    class StoryClicked(val id: ItemId) : StoryListEvent()

    @Poko
    class CommentsClick(val id: ItemId) : StoryListEvent()
    data object FavoriteClick : StoryListEvent()
    data object Refresh : StoryListEvent()
}