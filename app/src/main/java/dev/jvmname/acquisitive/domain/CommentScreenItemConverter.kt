package dev.jvmname.acquisitive.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.backbase.deferredresources.DeferredFormattedString
import com.backbase.deferredresources.DeferredFormattedString.Resource
import com.backbase.deferredresources.DeferredText
import com.backbase.deferredresources.text.withFormatArgs
import dev.jvmname.acquisitive.R.string.dead
import dev.jvmname.acquisitive.R.string.time_author
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
        comment: RankedComment,
        indentColorProvider: (depth: Int) -> Color,
    ): Comment {
        val time = comment.comment.time
            .periodUntil(Clock.System.now(), TimeZone.currentSystemDefault())
            .toAbbreviatedDuration()
        return Comment(
            id = comment.comment.id,
            author = Pair(
                first = if (comment.comment.dead == true) Resource(dead)
                else DeferredFormattedString.Constant("%s"),
                second = when {
                    comment.comment.by != null -> Resource(time_author)
                        .withFormatArgs(time, comment.comment.by)

                    else -> DeferredText.Constant(time)
                }
            ),
            time = time,
            text = when {
                comment.comment.dead ?: false -> "[dead]" + comment.comment.text
                comment.comment.deleted ?: false -> "[deleted]" + comment.comment.text
                else -> comment.comment.text.orEmpty()
            },
            rank = comment.rank,
            numChildren = comment.comment.kids?.size ?: 0,
            expanded = comment.expanded,
            parent = comment.comment.parent,
            indentDepth = 20.dp * comment.depth,
            indentColor = indentColorProvider(comment.depth),
        )
    }
}