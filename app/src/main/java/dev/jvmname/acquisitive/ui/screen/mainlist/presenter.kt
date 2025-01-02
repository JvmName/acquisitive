package dev.jvmname.acquisitive.ui.screen.mainlist

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.getDisplayedTitle
import dev.jvmname.acquisitive.network.model.score
import dev.jvmname.acquisitive.repo.StoryItemRepo
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.toDateTimePeriod
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

@[Inject CircuitInject(MainListScreen::class, AppScope::class)]
class MainScreenPresenter(
    private val repo: StoryItemRepo,
    @Assisted private val screen: MainListScreen,
    @Assisted private val navigator: Navigator,
) : Presenter<MainListScreen.MainListState> {
    @Composable
    override fun present(): MainListScreen.MainListState {
        val now = remember { Clock.System.now() }
        val fetchMode by remember { mutableStateOf(screen.fetchMode) }
        val items by repo.observeStories(fetchMode)
            .collectAsState(emptyList(), Dispatchers.IO)

        val screenItems = items.mapIndexed({ i, item ->
            val isHot = item.score >= fetchMode.hotThreshold
            val icon = when (item) {
                is HnItem.Job -> "💼"
                is HnItem.Poll -> "🗳️"
                else -> null
            }

            val time = (now - item.time).toDateTimePeriod().toAbbreviatedDuration()
            val urlHost = when (item) {
                is HnItem.Job -> item.url?.let(::extractUrlHost)
                is HnItem.Story -> item.url?.let(::extractUrlHost)
                else -> null
            }

            item.toScreenItem(
                rank = i + 1,
                isHot = isHot,
                icon = icon,
                time = time,
                urlHost = urlHost,
            )
        })
        return MainListScreen.MainListState(fetchMode, screenItems) { event ->
            when (event) {
                MainListEvent.AddComment -> navigator
                MainListEvent.CommentsClick -> TODO()
                MainListEvent.FavoriteClick -> TODO()
                MainListEvent.UpvoteClick -> TODO()
            }
        }
    }

    companion object {
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

fun HnItem.toScreenItem(
    rank: Int,
    isHot: Boolean,
    time: String,
    urlHost: String?,
    icon: String? = null,
): HnScreenItem = when (this) {
    is HnItem.Comment -> HnScreenItem.CommentItem(
        text = text.orEmpty(),
        time = time,
        author = by.orEmpty(),
        numChildren = kids?.size ?: 0,
        parent = ItemId(parent)
    )

    else -> HnScreenItem.StoryItem(
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