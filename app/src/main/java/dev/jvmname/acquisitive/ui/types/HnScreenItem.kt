package dev.jvmname.acquisitive.ui.types

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.backbase.deferredresources.DeferredText
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.network.model.ItemId

@Immutable
sealed interface HnScreenItem {
    val id: ItemId

    @Poko
    class Story(
        override val id: ItemId,
        val title: String,
        val titleSuffix: String?,
        val isHot: Boolean,
        val score: String,
        val rank: String,
        val numChildren: Int,
        val author: DeferredText,
        val time: String,
        val urlHost: String?,
        val favicon: Favicon,
        val isDead: Boolean,
        val isDeleted: Boolean,
    ) : HnScreenItem {
    }

    @Poko
    class Comment(
        override val id: ItemId,
        val text: String,
        val time: String,
        val author: DeferredText,
        val numChildren: Int,
        val parent: ItemId,
        val indentDepth: Dp,
        val indentColor: Color,
        val expanded: Boolean,
        val rank: Int,
    ) : HnScreenItem
}

@Immutable
sealed interface Favicon {
    @JvmInline
    value class Icon(val url: String) : Favicon

    @JvmInline
    value class Default(val vector: ImageVector) : Favicon
}