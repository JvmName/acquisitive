package dev.jvmname.acquisitive.ui.types

import androidx.compose.runtime.Immutable
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.network.model.ItemId

@Immutable
sealed interface HnScreenItem {
    val id: ItemId

    @[Poko Immutable]
    class Shallow(override val id: ItemId) : HnScreenItem

    @[Poko Immutable]
    class StoryItem(
        override val id: ItemId,
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
        override val id: ItemId,
        val text: String,
        val time: String,
        val author: String,
        val numChildren: Int,
        val parent: ItemId,
    ) : HnScreenItem
}