package dev.jvmname.acquisitive.ui.types

import androidx.compose.ui.graphics.vector.ImageVector
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.network.model.ItemId

sealed interface HnScreenItem {

    @Poko
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

    @Poko
    class CommentItem(
        val text: String,
        val time: String,
        val author: String,
        val numChildren: Int,
        val parent: ItemId,
    ) : HnScreenItem
}