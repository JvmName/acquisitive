package dev.jvmname.acquisitive.ui.screen.comments

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.backbase.deferredresources.DeferredText
import com.backbase.deferredresources.compose.rememberResolvedString
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.jvmname.acquisitive.dev.AcqPreview
import dev.jvmname.acquisitive.dev.previewComment
import dev.jvmname.acquisitive.dev.previewStoryItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.ui.common.Favicon
import dev.jvmname.acquisitive.ui.screen.comments.CommentListScreen.CommentListState
import dev.jvmname.acquisitive.ui.theme.Typography
import dev.jvmname.acquisitive.ui.types.Favicon
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import dev.zacsweers.metro.AppScope

@[Composable CircuitInject(CommentListScreen::class, AppScope::class)]
fun CommentListContent(contentState: CommentListState, modifier: Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {},
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = modifier
                .padding(top = 16.dp)
                .padding(paddingValues),
            isRefreshing = contentState is CommentListState.Loading,
            onRefresh = {
                if (contentState !is CommentListState.Full) return@PullToRefreshBox
                contentState.eventSink(CommentListEvent.Refresh)
            }
        ) {
            AnimatedContent(contentState) { state ->
                LazyColumn(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .fillMaxSize()
                ) {
                    when (state) {
                        is CommentListState.Loading -> {
                            item(key = 1, contentType = { "loading" }) {
                                Box {
                                    Progress(Modifier.padding(top = 16.dp))
                                }
                            }
                        }

                        is CommentListState.Full -> {
                            val story = state.storyItem
                            val comments = state.commentItems
                            item(
                                key = story.id,
                                contentType = { "story" }) {
                                StoryCardItem(
                                    story = story,
                                    onStoryClick = { state.eventSink(CommentListEvent.StoryClicked) }
                                )
                            }
                            items(
                                items = comments,
                                contentType = { "comments" },
                                key = { it.id },
                            ) {
                                CommentItem(
                                    comment = it,
                                    onToggleExpand = {
                                        state.eventSink(CommentListEvent.ExpandToggled(it))
                                    })
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun BoxScope.Progress(modifier: Modifier) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .align(Alignment.Center)
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(vertical = 24.dp)
                .align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}


@Composable
fun StoryCardItem(
    modifier: Modifier = Modifier,
    story: HnScreenItem.Story,
    onStoryClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        shape = CutCornerShape(topEnd = 12.dp),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = onStoryClick,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, 8.dp)) {
            Text(
                text = story.title, style = Typography.headlineMedium
            )

            if (story.urlHost != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Favicon(story)
                    Spacer(Modifier.size(3.dp))
                    Text(
                        text = story.urlHost, style = Typography.labelSmall
                    )
                }
            }
            Spacer(Modifier.size(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconText(icon = Icons.Outlined.ThumbUp, text = story.score)
                IconText(
                    icon = Icons.AutoMirrored.Outlined.Comment, text = story.numChildren.toString()
                )
                IconText(icon = Icons.Outlined.AccessTime, text = story.time)
                IconText(icon = Icons.Outlined.AccountCircle, text = rememberResolvedString(story.author).substringAfter('•').trim()) //laziness
            }
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
            .padding(horizontal = 16.dp, vertical = 5.dp), badge = {
            if (comment.numChildren == 0 || comment.expanded) return@BadgedBox
            Badge(containerColor = comment.indentColor) {
                Text(text = "+${comment.numChildren}")
            }
        }) {
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
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
                            text = rememberResolvedString(comment.author),
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = comment.time,
                            style = Typography.labelSmall,
                        )
                    }

                    Text(
                        text = AnnotatedString.fromHtml(comment.text),
                        style = Typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    ) //todo html / markdown
                }
            }
        }
    }
}


@Composable
fun IconText(modifier: Modifier = Modifier, icon: ImageVector, text: String) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, "", Modifier.size(16.dp)
        )
        Text(text = text, style = Typography.labelMedium)
    }
}

////////////////////////////////////////////////////////////////////////////
// previews
///////////////////////////////////////////////////////////////////////////


@PreviewLightDark
@Composable
fun StoryCardPreview() {
    val story = HnScreenItem.Story(
        id = ItemId(1),
        title = "Sample Story Title",
        titleSuffix = null,
        isHot = false,
        score = "123",
        rank = "1",
        numChildren = 42,
        author = DeferredText.Constant("Sample Author"),
        time = "2h ago",
        urlHost = "example.com",
        favicon = Favicon.Default(Icons.Default.Public),
        isDead = false,
        isDeleted = false
    )
    AcqPreview {
        StoryCardItem(story = story, onStoryClick = {})
    }
}

@PreviewLightDark
@Composable
fun Loading() {
    AcqPreview {
        CommentListContent(CommentListState.Loading, Modifier)
    }
}

@PreviewLightDark
@Composable
private fun CommentListPreview() {
    AcqPreview {
        CommentListContent(
            contentState = CommentListState.Full(
                isRefreshing = false,
                storyItem = previewStoryItem(111),
                commentItems = listOf(
                    previewComment(id = 1, depth = 0, rank = 1, expanded = false),
                    previewComment(id = 2, depth = 0, rank = 2, expanded = false),
                    previewComment(id = 3, depth = 0, rank = 3, expanded = false),
                    previewComment(id = 4, depth = 0, rank = 4, expanded = false),
                    previewComment(id = 5, depth = 0, rank = 5, expanded = false),
                    previewComment(id = 6, depth = 0, rank = 6, expanded = false),
                ),
                eventSink = { },
            ),
            modifier = Modifier
        )
    }
}

@PreviewLightDark
@Composable
private fun CommentListDeepNestingPreview() {
    AcqPreview {
        CommentListContent(
            contentState = CommentListState.Full(
                isRefreshing = false,
                storyItem = previewStoryItem(111),
                commentItems = listOf(
                    previewComment(id = 1, depth = 0, rank = 1),
                    previewComment(id = 2, depth = 1, rank = 2),
                    previewComment(id = 3, depth = 2, rank = 3),
                    previewComment(id = 4, depth = 3, rank = 4),
                    previewComment(id = 5, depth = 4, rank = 5),
                    previewComment(id = 6, depth = 5, rank = 6),
                    previewComment(id = 7, depth = 2, rank = 7),
                    previewComment(id = 8, depth = 3, rank = 8),
                ),
                eventSink = { },
            ),
            modifier = Modifier
        )
    }
}