package dev.jvmname.acquisitive.ui.screen.mainlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.ui.theme.AcquisitiveTheme
import dev.jvmname.acquisitive.ui.theme.hotColor
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import logcat.logcat
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

private val MIN_ITEM_HEIGHT = 200.dp

@Parcelize
data class MainListScreen(val fetchMode: FetchMode = FetchMode.TOP) : Screen {
    data class MainListState(
        val fetchMode: FetchMode,
        val stories: List<HnScreenItem>,
        val inflateItemsAfter: MutableStateFlow<Int>,
        val eventSink: (MainListEvent) -> Unit,
    ) : CircuitUiState
}

sealed class MainListEvent : CircuitUiEvent {
    data object FavoriteClick : MainListEvent()
    data object UpvoteClick : MainListEvent()
    data object CommentsClick : MainListEvent()
    data object AddComment : MainListEvent()
}

@[Composable CircuitInject(MainListScreen::class, AppScope::class)]
fun MainListContent(state: MainListScreen.MainListState, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(state.fetchMode.name) }) }) { innerPadding ->

        val scrollState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            state = scrollState
        ) {
            logcat { "***list size: ${state.stories.size}; shallow: ${state.stories.count { it is HnScreenItem.Shallow }} other: ${state.stories.count { it !is HnScreenItem.Shallow }}" }
            items(state.stories,
                key = {
                    when (it) {
                        is HnScreenItem.Shallow -> it.id
                        is HnScreenItem.StoryItem -> it.id
                        is HnScreenItem.CommentItem -> error("unexpected item type")
                    }
                }) { item ->
                MainListItem(modifier, item, state.eventSink)
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        val lifecycle = remember { lifecycleOwner.lifecycle }
        LaunchedEffect(scrollState, state) {
//            snapshotFlow { scrollState.layoutInfo }
//                .filter { layoutInfo ->
//                    when{
//                        scrollState.canScrollForward -> true
//                        layoutInfo.visibleItemsInfo.lastOrNull()?.index >= state.stories.lastIndex -> true
//                        else -> false
//
//                    }
//                }


            snapshotFlow { scrollState.firstVisibleItemIndex }
                .filter { topIndex ->
                    logcat { "***saw scroll to $topIndex" }
                    when {
                        !scrollState.canScrollForward -> true
                        state.stories.size - 1 > topIndex -> true
                        else -> state.stories.subList(
                            topIndex,
                            scrollState.layoutInfo.visibleItemsInfo.last().index
                        )
                    }

                    if (state.stories.lastIndex > topIndex) {
                        state.stories[topIndex] is HnScreenItem.Shallow
                    } else false
                }
                .flowWithLifecycle(lifecycle)
                .collect { index ->
                    logcat { "***updated scroll to $index" }
                    state.inflateItemsAfter.update { index }
                }
        }
    }
}

@Composable
fun MainListItem(
    modifier: Modifier,
    item: HnScreenItem,
    eventSink: (MainListEvent) -> Unit,
) {
    val f =
        remember { logcat("MainListItem") { "entered MLI with ${item::class.simpleName}" }; false }
    //TODO: fork Swipe and put the mutative actions on the righthand side
    when (item) {
        is HnScreenItem.Shallow -> CircularProgressIndicator(
            modifier = Modifier.width(64.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        is HnScreenItem.StoryItem -> OutlinedCard(modifier = modifier) {
            ConstraintLayout(
                modifier = modifier
                    .heightIn(max = MIN_ITEM_HEIGHT)
                    .fillMaxWidth()
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
                            width = Dimension.preferredWrapContent
                            height = Dimension.fillToConstraints
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 10.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        item.rank.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        item.score.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isHot) hotColor else Color.Unspecified,
                        fontWeight = if (item.isHot) FontWeight.Bold else LocalTextStyle.current.fontWeight
                    )
                    if (item.isHot) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            "hot",
                            tint = hotColor,
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
                    Text(
                        item.urlHost, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.constrainAs(urlHost) {
                            top.linkTo(title.bottom)
                            start.linkTo(rankScoreBox.end, margin = 8.dp)
                            end.linkTo(actionBox.start)
                            width = Dimension.fillToConstraints
                        },
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis

                    )
                } else {
                    Spacer(Modifier.constrainAs(urlHost) {
                        top.linkTo(title.bottom)
                        start.linkTo(rankScoreBox.end, margin = 8.dp)
                    })
                }

                Text(
                    (if (item.isDead) "[dead]\n" else "") + "${item.time} - ${item.author}",
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
                            Icon(Icons.Outlined.KeyboardDoubleArrowUp, "Upvote")
                        }
                    }

                    TextButton(
                        onClick = {
                            eventSink(MainListEvent.CommentsClick)

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

@[Preview Composable]
fun PreviewMainList() {
    val state = remember {
        val list = List(15) {
            storyItem(it)
        }
        MainListScreen.MainListState(FetchMode.TOP, list, MutableStateFlow(-1)) {}
    }
    AcquisitiveTheme(darkTheme = true) {
        MainListContent(state)
    }
}

@[Preview Composable]
fun PreviewMainListItem() {
    AcquisitiveTheme(darkTheme = true) {
        MainListItem(
            Modifier,
            storyItem(1234),
            eventSink = {}
        )
    }
}

private fun storyItem(it: Int) = HnScreenItem.StoryItem(
    id = ItemId(it),
    title = "Archimedes, Vitruvius, and Leonardo: The Odometer Connection (2020)",
    isHot = true,
    rank = it + 1,
    score = 950,
    urlHost = "github.com",
    numChildren = 121 + it,
    time = "19h",
    author = "JvmName",
    isDead = false,
    isDeleted = false,
    titleSuffix = "💼",
)