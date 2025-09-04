package dev.jvmname.acquisitive.ui.screen.mainlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Recommend
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import coil3.compose.AsyncImage
import com.backbase.deferredresources.DeferredFormattedString
import com.backbase.deferredresources.DeferredText
import com.backbase.deferredresources.compose.rememberResolvedString
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.jvmname.acquisitive.R
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.ui.ThemePreviews
import dev.jvmname.acquisitive.ui.common.LargeDropdownMenu
import dev.jvmname.acquisitive.ui.theme.AcquisitiveTheme
import dev.jvmname.acquisitive.ui.theme.hotColor
import dev.jvmname.acquisitive.ui.types.Favicon
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import dev.zacsweers.metro.AppScope
import kotlinx.coroutines.flow.flowOf
import logcat.LogPriority
import logcat.logcat

private val CELL_HEIGHT = 75.dp

@[Composable CircuitInject(MainListScreen::class, AppScope::class)]
fun MainListContent(state: MainListScreen.MainListState, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { FetchModeSwitcher(state) }) { innerPadding ->

        PullToRefreshBox(
            modifier = modifier
                .padding(top = 16.dp)
                .padding(innerPadding),
            isRefreshing = state.isRefreshing,
            onRefresh = { state.eventSink(MainListEvent.Refresh) }
        ) {
            when (state.pagedStories.loadState.refresh) {
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
                                MainListItem(
                                    modifier,
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

@Composable
private fun FetchModeSwitcher(state: MainListScreen.MainListState) {
    val resources = LocalResources.current
    TopAppBar(
        modifier = Modifier.wrapContentHeight(),
        title = {
            LargeDropdownMenu(
                modifier = Modifier.fillMaxWidth(0.55f),
                items = FetchMode.entries.fastMap {
                    resources.getString(R.string.stories_dropdown, it)
                },
                selectedIndex = FetchMode.entries.indexOf(state.fetchMode),
                onItemSelected = { i, _ ->
                    state.eventSink(MainListEvent.FetchModeChanged(FetchMode.entries[i]))
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
fun MainListItem(
    modifier: Modifier,
    item: HnScreenItem,
    eventSink: (MainListEvent) -> Unit,
) {
    when (item) {
        is HnScreenItem.StoryItem -> OutlinedCard(modifier = modifier) {
            ConstraintLayout(
                modifier = modifier
                    .heightIn(max = CELL_HEIGHT)
                    .fillMaxWidth()
                    .clickable { item.urlHost?.let { eventSink(MainListEvent.ItemClicked(item.id)) } }
            ) {
                val (rankScoreBox, actionBox) = createRefs()
                val (title, urlHost, timeAuthor) = createRefs()

                val startGuide = createGuidelineFromStart(8.dp)
                val endGuide = createGuidelineFromEnd(8.dp)

                Column(
                    modifier
                        .constrainAs(rankScoreBox) {
                            top.linkTo(parent.top)
                            start.linkTo(startGuide)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.preferredWrapContent.atLeast(47.dp)
                            height = Dimension.fillToConstraints
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 10.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        item.rank.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        item.score.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isHot) MaterialTheme.colorScheme.hotColor else Color.Unspecified,
                        fontWeight = if (item.isHot) FontWeight.Bold else LocalTextStyle.current.fontWeight
                    )
                    if (item.isHot) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            "hot",
                            tint = MaterialTheme.colorScheme.hotColor,
                        )
                    }
                }

                Text(
                    buildTitleText(item),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .constrainAs(title) {
                            top.linkTo(parent.top, margin = 8.dp)
                            start.linkTo(rankScoreBox.end, margin = 8.dp)
                            end.linkTo(endGuide)
                            width = Dimension.fillToConstraints
                        }
                )

                if (item.urlHost != null) {
                    Row(
                        Modifier.constrainAs(urlHost) {
                            top.linkTo(title.bottom)
                            start.linkTo(rankScoreBox.end, margin = 8.dp)
                            end.linkTo(actionBox.start)
                            width = Dimension.fillToConstraints
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (item.favicon) {
                            is Favicon.Icon -> AsyncImage(
                                model = item.favicon.url,
                                contentDescription = "favicon",
                                modifier = Modifier.size(14.dp),
                                fallback = rememberVectorPainter(Icons.Default.Public)
                            )

                            is Favicon.Default -> Icon(
                                item.favicon.vector,
                                "",
                                Modifier.size(14.dp)
                            )
                        }
                        Spacer(Modifier.size(3.dp))
                        Text(
                            item.urlHost, style = MaterialTheme.typography.labelSmall,
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
                    text = item.authorInfo.let { (dead, author) ->
                        rememberResolvedString(dead, rememberResolvedString(author))
                    },
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
                    if (!item.isDeleted && !item.isDead) {
                        IconButton(onClick = {
                            eventSink(MainListEvent.FavoriteClick)
                        }) {
                            Icon(Icons.Outlined.FavoriteBorder, "Favorite")
                        }

                        IconButton(onClick = {
                            eventSink(MainListEvent.UpvoteClick)

                        }) {
                            Icon(Icons.Outlined.Recommend, "Upvote")
                        }
                    }

                    TextButton(
                        onClick = {
                            eventSink(MainListEvent.CommentsClick(item.id))

                        },
                        colors = with(
                            IconButtonDefaults.iconButtonColors()
                        ) {
                            ButtonColors(
                                containerColor = containerColor,
                                contentColor = contentColor,
                                disabledContainerColor = disabledContainerColor,
                                disabledContentColor = disabledContentColor
                            )
                        },
                    ) {
                        val icon = if (item.isHot) Icons.Outlined.LocalFireDepartment
                        else Icons.AutoMirrored.Outlined.Comment

                        val cachedColor = LocalContentColor.current
                        val hotColor = MaterialTheme.colorScheme.hotColor
                        CompositionLocalProvider(LocalContentColor.providesComputed {
                            if (item.isHot) hotColor
                            else cachedColor
                        }) {
                            Icon(icon, "Comments")
                            if (item.numChildren > 0) {
                                Text(
                                    item.numChildren.toString(),
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        }
                    }

                    if (!item.isDeleted && !item.isDead) {
                        IconButton(onClick = { eventSink(MainListEvent.AddComment) }) {
                            Icon(Icons.Outlined.AddComment, "Comment")
                        }
                    }
                }
            }
        }

        is HnScreenItem.CommentItem -> TODO("won't happen")
    }
}

@Composable
private fun buildTitleText(item: HnScreenItem.StoryItem): String {
    val title = AnnotatedString.fromHtml(item.title)
    val icon = item.titleSuffix.orEmpty()
    //normally wouldn't be this fussy but everything here is inside a list-loop
    return buildString(title.length + 1 + icon.length) {
        append(title)
        append(" ")
        append(icon)
    }
}

@Composable
@ThemePreviews
fun PreviewMainListItem() {
    AcquisitiveTheme {
        MainListItem(
            Modifier,
            storyItem(1234),
            eventSink = {}
        )
    }
}

@Composable
@ThemePreviews
fun PreviewMainList() {
    val paged = remember {
        val nl = LoadState.NotLoading(true)
        flowOf(
            PagingData.from(
                List(15) { storyItem(it) },
                sourceLoadStates = LoadStates(nl, nl, nl),
                mediatorLoadStates = LoadStates(nl, nl, nl),

                )
        )
    }.collectAsLazyPagingItems()


    val state = remember {
        MainListScreen.MainListState(false, FetchMode.TOP, paged) {}
    }
    AcquisitiveTheme {
        MainListContent(state)
    }
}

private fun storyItem(id: Int): HnScreenItem = HnScreenItem.StoryItem(
    id = ItemId(id),
    title = "Archimedes, Vitruvius, and Leonardo: The Odometer Connection (2020)",
    isHot = true,
    score = 950,
    urlHost = "github.com",
    favicon = Favicon.Default(Icons.Default.Public),
    numChildren = 121 + id,
    authorInfo = Pair(
        DeferredFormattedString.Constant("%s"),
        DeferredText.Constant("19h - JvmName")
    ),
    isDead = false,
    isDeleted = false,
    titleSuffix = "💼",
    rank = id
)