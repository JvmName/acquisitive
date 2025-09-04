package dev.jvmname.acquisitive.ui.screen.main

import androidx.paging.compose.LazyPagingItems
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class MainListScreen(val fetchMode: FetchMode = FetchMode.TOP) : Screen {
    data class MainListState(
        val isRefreshing: Boolean,
        val fetchMode: FetchMode,
        val pagedStories: LazyPagingItems<HnScreenItem>,
        val eventSink: (MainListEvent) -> Unit,
    ) : CircuitUiState
}

sealed class MainListEvent : CircuitUiEvent {
    data class FetchModeChanged(val fetchMode: FetchMode) : MainListEvent()
    data class ItemClicked(val id: ItemId) : MainListEvent()
    data class CommentsClick(val id: ItemId) : MainListEvent()
    data object FavoriteClick : MainListEvent()
    data object Refresh : MainListEvent()
}