package dev.jvmname.acquisitive.ui.screen.mainlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.ui.theme.AcquisitiveTheme
import dev.jvmname.acquisitive.ui.theme.primaryDarkMediumContrast
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import kotlinx.parcelize.Parcelize
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

private val MIN_ITEM_HEIGHT = 200.dp

@Parcelize
data class MainListScreen(val fetchMode: FetchMode = FetchMode.TOP) : Screen {
    data class MainListState(
        val fetchMode: FetchMode,
        val stories: List<HnScreenItem>,
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
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(state.stories) { item ->
                MainListItem(modifier, item as HnScreenItem.StoryItem, state.eventSink)
            }
        }
    }
}

@Composable
fun MainListItem(
    modifier: Modifier,
    item: HnScreenItem.StoryItem,
    eventSink: (MainListEvent) -> Unit,
) {
    OutlinedCard(modifier = modifier) {
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
                        width = Dimension.ratio("1:1")
                    }
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    item.rank.toString(),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    item.score.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isHot) primaryDarkMediumContrast else Color.Unspecified
                )
                if (item.isHot) {
                    Icon(Icons.Default.LocalFireDepartment, "hot", tint = primaryDarkMediumContrast)
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
                    CompositionLocalProvider(LocalContentColor provides primaryDarkMediumContrast) {
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
}

@Composable
private fun buildTitleText(item: HnScreenItem.StoryItem): String {
    val title = AnnotatedString.fromHtml(item.title)
    val icon = when {
        item.isDead -> "☠️"
        item.isDeleted -> "🗑️"
        else -> item.titleSuffix
    }
    //normally wouldn't be this fussy but everything here is inside a list-loop
    return buildString(title.length + 1 + (icon?.length ?: 0)) {
        append(title)
        append(" ")
        append(icon)
    }
}

@[Preview Composable]
fun PreviewMainListItem() {
    AcquisitiveTheme {
        MainListItem(
            Modifier,
            HnScreenItem.StoryItem(
                id = ItemId(123),
                title = "Archimedes, Vitruvius, and Leonardo: The Odometer Connection (2020)",
                isHot = false,
                rank = 2,
                score = 950,
                urlHost = "github.com",
                numChildren = 122,
                time = "19h",
                author = "JvmName",
                isDead = false,
                isDeleted = false,
                titleSuffix = "💼",
            ),
            eventSink = {}
        )
    }
}