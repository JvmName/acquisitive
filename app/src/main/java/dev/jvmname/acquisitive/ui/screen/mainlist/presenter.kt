package dev.jvmname.acquisitive.ui.screen.mainlist

import android.annotation.SuppressLint
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.theapache64.rebugger.Rebugger
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.ShadedHnItem
import dev.jvmname.acquisitive.network.model.getDisplayedTitle
import dev.jvmname.acquisitive.network.model.score
import dev.jvmname.acquisitive.repo.StoryItemRepo
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.toDateTimePeriod
import logcat.logcat
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

@[Inject CircuitInject(MainListScreen::class, AppScope::class)]
class MainScreenPresenter(
    private val repo: StoryItemRepo,
    @Assisted private val screen: MainListScreen,
    @Assisted private val navigator: Navigator,
) : Presenter<MainListScreen.MainListState> {
    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    override fun present(): MainListScreen.MainListState {
        val inflateChannel = remember { MutableStateFlow(-1) }

        val now = remember { Clock.System.now() }
        val fetchMode by remember { mutableStateOf(screen.fetchMode) }
        SideEffect { logcat { "***before repo.observeStories" } }
        val storyIds by repo.observeStories(fetchMode)
            .collectAsState(emptyList(), context = Dispatchers.IO)

        val updatedRange: Pair<IntRange, List<ShadedHnItem>>? by inflateChannel
            .filter { it >= 0 }
            .mapLatest { index ->
                logcat { "***consuming new index $index" }
                val range = index..index + INFLATE_WINDOW
                val sliced = storyIds.slice(range)
                if (sliced.all { it is ShadedHnItem.Full }) {
                    null // no need to update anything
                } else {
                    range to repo.getStories(sliced)
                }
            }
            .collectAsState(null, context = Dispatchers.IO)

        val items by remember {
            derivedStateOf {
                logcat { "***building new items list; currently ${storyIds.size} items" }
                logcat { "***storyIds1 = " + storyIds.debugToString1() }

                if (storyIds.isEmpty()) return@derivedStateOf emptyList<HnScreenItem>()
                val updated = updatedRange
                    ?.let { (range, updatedItems) ->
                        buildList {
                            storyIds.slice(0..range.first)
                            addAll(updatedItems)
                            addAll(storyIds.slice(range.last..storyIds.lastIndex))
                        }
                    } ?: storyIds

                updated.mapIndexed { i, shaded ->
                    val item = when (shaded) {
                        is ShadedHnItem.Shallow -> return@mapIndexed shaded.toScreenItem()
                        is ShadedHnItem.Full -> shaded.item
                    }

                    shaded.toScreenItem(
                        rank = i + 1,
                        isHot = item.score >= fetchMode.hotThreshold,
                        icon = when {
                            item.dead == true -> "☠️"
                            item.deleted == true -> "🗑️"
                            item is HnItem.Job -> "💼"
                            item is HnItem.Poll -> "🗳️"
                            else -> null
                        },
                        time = (now - item.time).toDateTimePeriod().toAbbreviatedDuration(),
                        urlHost = when (item) {
                            is HnItem.Job -> item.url?.let(::extractUrlHost)
                            is HnItem.Story -> item.url?.let(::extractUrlHost)
                            else -> null
                        },
                    )
                }
            }
        }

        Rebugger(
            trackMap = mapOf(
                "inflateChannel" to inflateChannel,
                "now" to now,
                "fetchMode" to fetchMode,
//                "storyIds" to storyIds ,
                "updatedRange" to updatedRange,
                "items" to items,
            ),
        )

        SideEffect {
            logcat { "***after rebugger before return" }
            logcat { "***storyIds2 = " + items.debugToString2() }
        }
        return MainListScreen.MainListState(
            fetchMode = fetchMode,
            stories = items,
            inflateItemsAfter = inflateChannel
        ) { event ->
            when (event) {
                MainListEvent.AddComment -> navigator
                MainListEvent.CommentsClick -> TODO()
                MainListEvent.FavoriteClick -> TODO()
                MainListEvent.UpvoteClick -> TODO()
            }
        }
    }


    companion object {
        private const val INFLATE_WINDOW = 10

        private const val HOT_THRESHOLD_HIGH = 900
        private const val HOT_THRESHOLD_NORMAL = 300
        private const val HOT_THRESHOLD_LOW = 30

        @VisibleForTesting
        internal val FetchMode.hotThreshold: Int
            get() = when (this) {
                FetchMode.BEST -> HOT_THRESHOLD_HIGH
                FetchMode.NEW -> HOT_THRESHOLD_LOW
                else -> HOT_THRESHOLD_NORMAL
            }

        @VisibleForTesting
        internal fun DateTimePeriod.toAbbreviatedDuration(): String = when {
            years > 0 -> "${years}y"
            months > 0 -> "${months}mo"
            days >= 7 -> "${days / 7}w"
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            seconds > 10 -> "${seconds}s"
            else -> "<1s"
        }

        @VisibleForTesting
        internal fun extractUrlHost(url: String): String = try {
            Uri.parse(url).host.orEmpty()
        } catch (e: Exception) {
            ""
        }
    }
}

fun ShadedHnItem.Shallow.toScreenItem(): HnScreenItem = HnScreenItem.Shallow(item)

fun ShadedHnItem.Full.toScreenItem(
    rank: Int,
    isHot: Boolean,
    time: String,
    urlHost: String?,
    icon: String? = null,
): HnScreenItem = when (item) {
    is HnItem.Comment -> with(item) {
        HnScreenItem.CommentItem(
            text = text.orEmpty(),
            time = time,
            author = by.orEmpty(),
            numChildren = kids?.size ?: 0,
            parent = ItemId(parent)
        )
    }

    else -> with(item) {
        HnScreenItem.StoryItem(
            id = id,
            title = getDisplayedTitle(),
            isHot = isHot,
            rank = rank,
            score = when (this) {
                is HnItem.Story -> score
                is HnItem.Job -> score
                is HnItem.Poll -> score
                is HnItem.PollOption -> score
                else -> 0
            },
            urlHost = urlHost,
            numChildren = kids?.size ?: 0,
            time = time,
            author = by.orEmpty(),
            isDead = dead ?: false,
            isDeleted = deleted ?: false,
            titleSuffix = icon
        )
    }
}


 fun List<ShadedHnItem>.debugToString1(): String = joinToString("") {
    when (it) {
        is ShadedHnItem.Full -> "F."
        is ShadedHnItem.Shallow -> "s."
    }
}

inline fun <reified T : HnScreenItem> List<T>.debugToString2(): String =
    joinToString("") {
        when (it) {
            is HnScreenItem.CommentItem -> "c."
            is HnScreenItem.Shallow -> "s."
            is HnScreenItem.StoryItem -> "S."
            else -> ""
        }
    }

