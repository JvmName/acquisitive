package dev.jvmname.acquisitive.ui.screen.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.backbase.deferredresources.DeferredFormattedString
import com.backbase.deferredresources.DeferredText
import com.backbase.deferredresources.compose.rememberResolvedString
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.ui.theme.indent
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import dev.jvmname.acquisitive.util.CommentMap
import dev.zacsweers.metro.AppScope

@Stable
class ChildFetcher(private val map: CommentMap<HnScreenItem.Comment>) :
        (ItemId) -> List<HnScreenItem.Comment> {
    override fun invoke(id: ItemId): List<HnScreenItem.Comment> = map[id] ?: emptyList()
}

@[Composable CircuitInject(CommentListScreen::class, AppScope::class)]
fun CommentListContent(state: CommentListScreen.CommentListState, modifier: Modifier) {
    TODO()
}

@Composable
fun StoryCard(modifier: Modifier = Modifier, story: HnScreenItem.Story) {

}

@Composable
fun CommentList(
    modifier: Modifier = Modifier,
    comments: List<HnScreenItem.Comment>,
    onToggleExpand: (ItemId) -> Unit,
) {
    LazyColumn(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        items(
            items = comments,
            key = { it.id },
        ) {
            CommentItem(comment = it, onToggleExpand = onToggleExpand)
        }
    }
}

@Composable
fun CommentItem(
    modifier: Modifier = Modifier,
    comment: HnScreenItem.Comment,
    onToggleExpand: (ItemId) -> Unit,
) {
    BadgedBox(
        modifier = modifier
            .clickable {
                if (comment.numChildren == 0) return@clickable
                onToggleExpand(comment.id)
            }
            .padding(horizontal = 16.dp, vertical = 5.dp),
        badge = {
            if (comment.numChildren == 0 || comment.expanded) return@BadgedBox
            Badge(containerColor = comment.indentColor) {
                Text(text = "+${comment.numChildren}")
            }
        }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = comment.indentDepth, end = 4.dp),
                thickness = 3.dp,
                color = comment.indentColor
            )
            Card(
//                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                shape = CutCornerShape(topEnd = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 0.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start)
                    ) {
                        Text(
                            text = comment.author.let { (dead, author) ->
                                rememberResolvedString(
                                    dead,
                                    rememberResolvedString(author)
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = comment.time,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    Text(
                        text = comment.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    ) //todo html / markdown
                }
            }
        }
    }
}

@Composable
private fun previewComment(
    id: Int,
    depth: Int,
    rank: Int,
    expanded: Boolean = true,
): HnScreenItem.Comment {
    return HnScreenItem.Comment(
        id = ItemId(id),
        text = "This is a sample comment at depth $depth with some example text. Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        time = "${(1..12).random()}:${(10..59).random()} ago",
        author = Pair(
            DeferredFormattedString.Constant("Sample Author"),
            DeferredText.Constant("user${(1000..9999).random()}")
        ),
        numChildren = if (depth < 3) (0..5).random() else 0,
        parent = ItemId(id - 1),
        indentDepth = (depth * 20).dp,
        indentColor = MaterialTheme.colorScheme.indent(depth),
        expanded = expanded,
        rank = rank
    )
}

@PreviewLightDark
@Composable
private fun CommentItemPreview() {
    CommentItem(
        comment = previewComment(id = 1, depth = 0, rank = 1),
        onToggleExpand = {}
    )
}

@PreviewLightDark
@Composable
private fun CommentItemDepthPreview() {
    CommentItem(
        comment = previewComment(id = 2, depth = 3, rank = 2),
        onToggleExpand = {}
    )
}

@PreviewLightDark
@Composable
private fun CommentListPreview() {
    CommentList(
        comments = listOf(
            previewComment(id = 1, depth = 0, rank = 1, expanded = false),
            previewComment(id = 2, depth = 0, rank = 2, expanded = false),
            previewComment(id = 3, depth = 0, rank = 3, expanded = false),
            previewComment(id = 4, depth = 0, rank = 4, expanded = false),
            previewComment(id = 5, depth = 0, rank = 5, expanded = false),
            previewComment(id = 6, depth = 0, rank = 6, expanded = false),
        ),
        onToggleExpand = {}
    )
}

@PreviewLightDark
@Composable
private fun CommentListDeepNestingPreview() {
    CommentList(
        comments = listOf(
            previewComment(id = 1, depth = 0, rank = 1),
            previewComment(id = 2, depth = 1, rank = 2),
            previewComment(id = 3, depth = 2, rank = 3),
            previewComment(id = 4, depth = 3, rank = 4),
            previewComment(id = 5, depth = 4, rank = 5),
            previewComment(id = 6, depth = 5, rank = 6),
            previewComment(id = 7, depth = 2, rank = 7),
            previewComment(id = 8, depth = 3, rank = 8),
        ),
        onToggleExpand = {}
    )
}