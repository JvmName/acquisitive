package dev.jvmname.acquisitive.domain

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.mikepenz.markdown.model.MarkdownState
import dev.jvmname.acquisitive.repo.comment.RankedComment
import dev.jvmname.acquisitive.ui.types.HnScreenItem.Comment
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

@AssistedInject
class CommentScreenItemConverter(
    @Assisted private val indentColorProvider: (depth: Int) -> Color,
    @Assisted private val markdownProvider: @Composable (text: String) -> MarkdownState,
) : ItemConverter() {

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            indentColorProvider: (depth: Int) -> Color,
            markdownProvider: @Composable (text: String) -> MarkdownState,
        ): CommentScreenItemConverter
    }

    operator fun invoke(ranked: RankedComment): Comment {
        val comment = ranked.comment

        return Comment(
            id = comment.id,
            author = formatAuthor(comment.by, comment.dead),
            time = formatTime(comment.time),
            text = {
                val text = when {
                    comment.deleted ?: false -> "[deleted]\n" + comment.text
                    else -> comment.text.orEmpty()
                }

                val html = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
                markdownProvider(html.toString())
            },
            rank = ranked.rank,
            numChildren = comment.kids?.size ?: 0,
            expanded = ranked.expanded,
            expandable = !comment.kids.isNullOrEmpty(),
            parent = comment.parent,
            indentDepth = 20.dp * ranked.depth,
            indentColor = indentColorProvider(ranked.depth),
        )
    }
}