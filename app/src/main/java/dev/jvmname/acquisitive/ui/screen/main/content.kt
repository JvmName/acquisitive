package dev.jvmname.acquisitive.ui.screen.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.atLeast
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.cash.paging.PagingData
import com.backbase.deferredresources.compose.rememberResolvedString
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.jvmname.acquisitive.R
import dev.jvmname.acquisitive.dev.AcqPreview
import dev.jvmname.acquisitive.dev.previewStoryItem
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.ui.common.Favicon
import dev.jvmname.acquisitive.ui.common.LargeDropdownMenu
import dev.jvmname.acquisitive.ui.theme.hotColor
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import dev.jvmname.acquisitive.util.capitalize
import dev.zacsweers.metro.AppScope
import kotlinx.coroutines.flow.flowOf
import logcat.LogPriority
import logcat.logcat

private val CELL_HEIGHT = 75.dp

@[Composable CircuitInject(StoryListScreen::class, AppScope::class)]
fun StoryListUi(state: StoryListScreen.StoryListState, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { FetchModeSwitcher(state) }) { innerPadding ->
        PullToRefreshBox(
            modifier = modifier
                .padding(top = 16.dp)
                .padding(innerPadding),
            isRefreshing = state.isRefreshing,
            onRefresh = { state.eventSink(StoryListEvent.Refresh) }
        ) {
            AnimatedContent(state.pagedStories.loadState.refresh) { refreshState ->
                when (refreshState) {
                    is LoadState.Loading -> {
                        logcat(LogPriority.WARN) { "loadState: Loading" }
                        Progress(Modifier.padding(top = 16.dp))
                    }

                    else -> {
                        logcat(LogPriority.WARN) { "loadState: NotLoading" }
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            val paged = state.pagedStories
                            items(
                                count = paged.itemCount,
                                key = paged.itemKey { it.id },
                                itemContent = { index ->
                                    StoryListItem(
                                        modifier.animateItem(),
                                        paged[index] ?: return@items,
                                        state.eventSink
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FetchModeSwitcher(state: StoryListScreen.StoryListState) {
    val resources = LocalResources.current
    val entries = remember { FetchMode.entries }
    TopAppBar(
        modifier = Modifier.wrapContentHeight(),
        title = {
            LargeDropdownMenu(
                modifier = Modifier.fillMaxWidth(0.55f),
                items = entries.fastMap {
                    resources.getString(R.string.stories_dropdown, it.name.capitalize())
                },
                selectedIndex = entries.indexOf(state.fetchMode),
                onItemSelected = { i, _ ->
                    state.eventSink(StoryListEvent.FetchModeChanged(entries[i]))
                },
                label = "Mode",
            )
        })
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
fun StoryListItem(
    modifier: Modifier,
    story: HnScreenItem,
    eventSink: (StoryListEvent) -> Unit,
) {
    story as HnScreenItem.Story
    OutlinedCard(modifier = modifier) {
        ConstraintLayout(
            modifier = modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .clickable { story.urlHost?.let { eventSink(StoryListEvent.StoryClicked(story.id)) } }
        ) {
            val (rankScoreBox, actionBox) = createRefs()
            val (title, urlHost, timeAuthor) = createRefs()

            val startGuide = createGuidelineFromStart(8.dp)
            val endGuide = createGuidelineFromEnd(8.dp)

            Column(
                Modifier
                    .constrainAs(rankScoreBox) {
                        top.linkTo(parent.top)
                        start.linkTo(startGuide)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.preferredWrapContent.atLeast(47.dp)
                        height = Dimension.fillToConstraints
                    }
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 10.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    story.rank,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    story.score,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (story.isHot) MaterialTheme.colorScheme.hotColor else Color.Unspecified,
                    fontWeight = if (story.isHot) FontWeight.SemiBold else LocalTextStyle.current.fontWeight
                )
                if (story.isHot) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        "hot",
                        tint = MaterialTheme.colorScheme.hotColor,
                    )
                }
            }

            Text(
                buildTitleText(story),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .constrainAs(title) {
                        top.linkTo(parent.top, margin = 8.dp)
                        start.linkTo(rankScoreBox.end, margin = 8.dp)
                        end.linkTo(endGuide)
                        width = Dimension.fillToConstraints
                    }
            )

            if (story.urlHost != null) {
                Row(
                    Modifier.constrainAs(urlHost) {
                        top.linkTo(title.bottom)
                        start.linkTo(rankScoreBox.end, margin = 8.dp)
                        end.linkTo(actionBox.start)
                        width = Dimension.fillToConstraints
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Favicon(story)
                    Spacer(Modifier.size(3.dp))
                    Text(
                        story.urlHost, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis
                    )
                }

            } else {
                Spacer(Modifier.constrainAs(urlHost) {
                    top.linkTo(title.bottom)
                    start.linkTo(rankScoreBox.end, margin = 8.dp)
                })
            }

            Text(
                text = rememberResolvedString(story.author),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.constrainAs(timeAuthor) {
                    top.linkTo(urlHost.bottom)
                    start.linkTo(rankScoreBox.end, margin = 8.dp)
                    end.linkTo(actionBox.start)
                    width = Dimension.fillToConstraints
                },
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis
            )

            //TODO: fork Swipe and put the mutative actions on the righthand side
            Row(modifier = Modifier.constrainAs(actionBox) {
                top.linkTo(title.bottom)
                end.linkTo(endGuide)
                bottom.linkTo(timeAuthor.bottom)
                height = Dimension.wrapContent
                width = Dimension.wrapContent
            }) {
                if (!story.isDeleted && !story.isDead) {
                    IconButton(onClick = {
                        eventSink(StoryListEvent.FavoriteClick)
                    }) {
                        Icon(Icons.Outlined.FavoriteBorder, "Favorite")
                    }
                }

                TextButton(
                    onClick = { eventSink(StoryListEvent.CommentsClick(story.id)) },
                    colors = with(IconButtonDefaults.iconButtonColors()) {
                        ButtonColors(
                            containerColor = containerColor,
                            contentColor = contentColor,
                            disabledContainerColor = disabledContainerColor,
                            disabledContentColor = disabledContentColor
                        )
                    },
                ) {
                    val icon = if (story.isHot) Icons.Outlined.LocalFireDepartment
                    else Icons.AutoMirrored.Outlined.Comment

                    val cachedColor = LocalContentColor.current
                    val hotColor = MaterialTheme.colorScheme.hotColor
                    CompositionLocalProvider(LocalContentColor.providesComputed {
                        if (story.isHot) hotColor
                        else cachedColor
                    }) {
                        Icon(icon, "Comments")
                        if (story.numChildren > 0) {
                            Text(
                                story.numChildren.toString(),
                                modifier = Modifier
                                    .padding(start = 2.dp)
                                    .width(33.dp)
                            )
                        } else {
                            Spacer(Modifier.width(33.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun buildTitleText(item: HnScreenItem.Story): AnnotatedString = remember {
    val title = AnnotatedString.fromHtml(item.title)
    val icon = item.titleSuffix.orEmpty()
    //normally wouldn't be this fussy but everything here is inside a list-loop
    AnnotatedString.Builder(title.length + 1 + icon.length)
        .apply {
            append(title)
            append(" ")
            append(icon)
        }
        .toAnnotatedString()
}

@Composable
@PreviewLightDark
fun PreviewStoryListItem() {
    AcqPreview {
        StoryListItem(
            Modifier,
            previewStoryItem(1234),
            eventSink = {}
        )
    }
}

@Composable
@PreviewLightDark
fun PreviewStoryList() {
    val paged = remember {
        val nl = LoadState.NotLoading(true)
        flowOf(
            PagingData.from(
                List(15) { previewStoryItem(it) },
                sourceLoadStates = LoadStates(nl, nl, nl),
                mediatorLoadStates = LoadStates(nl, nl, nl),
            )
        )
    }.collectAsLazyPagingItems()


    val state = remember {
        StoryListScreen.StoryListState(false, FetchMode.TOP, paged) {}
    }
    AcqPreview {
        StoryListUi(state)
    }
}