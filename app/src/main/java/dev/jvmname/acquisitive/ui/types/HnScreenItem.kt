package dev.jvmname.acquisitive.ui.types

import androidx.compose.runtime.Immutable
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.getDisplayedTitle

@Immutable
sealed interface HnScreenItem {
    val id: ItemId

    @[Poko Immutable]
    class StoryItem(
        override val id: ItemId,
        val title: String,
        val isHot: Boolean,
        val score: Int,
        val rank: Int,
        val urlHost: UrlAndHost?,
        val numChildren: Int,
        val time: String,
        val author: String,
        val isDead: Boolean,
        val isDeleted: Boolean,
        val titleSuffix: String?,
    ) : HnScreenItem{
    }

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

@Immutable
@Poko
class UrlAndHost(
    val url: String,
    val host: String,
)

fun HnItem.toScreenItem(
    isHot: Boolean,
    time: String,
    rank: Int,
    urlHost: UrlAndHost?,
    icon: String? = null,
): HnScreenItem = when (this) {
    is HnItem.Comment -> HnScreenItem.CommentItem(
        id = id,
        text = text.orEmpty(),
        time = time,
        author = by.orEmpty(),
        numChildren = kids?.size ?: 0,
        parent = parent
    )

    else -> HnScreenItem.StoryItem(
        id = id,
        title = getDisplayedTitle(),
        isHot = isHot,
        score = when (this) {
            is HnItem.Story -> score
            is HnItem.Job -> score
            is HnItem.Poll -> score
            is HnItem.PollOption -> score
            else -> 0
        },
        rank = rank,
        urlHost = urlHost,
        numChildren = kids?.size ?: 0,
        time = time,
        author = by.orEmpty(),
        isDead = dead ?: false,
        isDeleted = deleted ?: false,
        titleSuffix = icon
    )
}