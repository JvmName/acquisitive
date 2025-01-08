package dev.jvmname.acquisitive.ui.types

import androidx.compose.runtime.Immutable
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.network.model.ItemId

@Immutable
sealed interface HnScreenItem {
    @[Poko Immutable]
    class Shallow(val id: ItemId) : HnScreenItem

    @[Poko Immutable]
    class StoryItem(
        val id: ItemId,
        val title: String,
        val isHot: Boolean,
        val rank: Int,
        val score: Int,
        val urlHost: String?,
        val numChildren: Int,
        val time: String,
        val author: String,
        val isDead: Boolean,
        val isDeleted: Boolean,
        val titleSuffix: String?,
    ) : HnScreenItem

    @[Poko Immutable]
    class CommentItem(
        val text: String,
        val time: String,
        val author: String,
        val numChildren: Int,
        val parent: ItemId,
    ) : HnScreenItem
}