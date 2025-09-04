package dev.jvmname.acquisitive.ui.types

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.backbase.deferredresources.DeferredFormattedString
import com.backbase.deferredresources.DeferredText
import com.backbase.deferredresources.text.withFormatArgs
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.R
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
        val titleSuffix: String?,
        val isHot: Boolean,
        val score: Int,
        val rank: String,
        val numChildren: Int,
        val authorInfo: Pair<DeferredFormattedString, DeferredText>,
        val urlHost: String?,
        val favicon: Favicon,
        val isDead: Boolean,
        val isDeleted: Boolean,
    ) : HnScreenItem {
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

fun HnItem.toScreenItem(
    isHot: Boolean,
    time: String,
    rank: Int,
    urlHost: String?,
    suffixIcon: String? = null,
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
        rank = "$rank.",
        urlHost = urlHost,
        favicon = if (urlHost != null) {
            Favicon.Icon("https://icons.duckduckgo.com/ip3/$urlHost.ico")
        } else Favicon.Default(Icons.Default.Public),
        numChildren = kids?.size ?: 0,
        authorInfo = Pair(
            first = if (dead == true) DeferredFormattedString.Resource(R.string.dead)
            else DeferredFormattedString.Constant("%s"),
            second = when {
                by != null -> DeferredFormattedString.Resource(R.string.time_author)
                    .withFormatArgs(time, by!!)

                else -> DeferredText.Constant(time)
            }
        ),
        isDead = dead ?: false,
        isDeleted = deleted ?: false,
        titleSuffix = suffixIcon
    )
}

@Immutable
sealed interface Favicon {
    @JvmInline
    value class Icon(val url: String) : Favicon

    @JvmInline
    value class Default(val vector: ImageVector) : Favicon
}