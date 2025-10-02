package dev.jvmname.acquisitive.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.backbase.deferredresources.DeferredFormattedString
import com.backbase.deferredresources.DeferredText
import com.backbase.deferredresources.text.withFormatArgs
import dev.jvmname.acquisitive.R
import dev.jvmname.acquisitive.domain.StoryScreenItemConverter.Companion.toAbbreviatedDuration
import dev.jvmname.acquisitive.repo.comment.RankedComment
import dev.jvmname.acquisitive.ui.types.HnScreenItem.Comment
import dev.zacsweers.metro.Inject
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlin.time.Clock

@Inject
class CommentScreenItemConverter {
    operator fun invoke(
        ranked: RankedComment,
        indentColorProvider: (depth: Int) -> Color,
    ): Comment {
        val comment = ranked.comment

        val time = comment.time
            .periodUntil(Clock.System.now(), TimeZone.currentSystemDefault())
            .toAbbreviatedDuration()
        val authorInfo = if (comment.dead == true) {
            DeferredFormattedString.Resource(R.string.dead)
                .withFormatArgs(comment.by.orEmpty())
        } else {
            DeferredText.Constant(comment.by.orEmpty())
        }
        return Comment(
            id = comment.id,
            author = authorInfo,
            time = time,
            text = when {
                comment.deleted ?: false -> "[deleted]\n" + comment.text
                else -> comment.text.orEmpty()
            },
            rank = ranked.rank,
            numChildren = comment.kids?.size ?: 0,
            expanded = ranked.expanded,
            parent = comment.parent,
            indentDepth = 20.dp * ranked.depth,
            indentColor = indentColorProvider(ranked.depth),
        )
    }
}