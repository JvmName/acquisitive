package dev.jvmname.acquisitive.domain

import androidx.annotation.VisibleForTesting
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.core.net.toUri
import com.backbase.deferredresources.DeferredFormattedString
import com.backbase.deferredresources.text.withFormatArgs
import dev.jvmname.acquisitive.R
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.descendants
import dev.jvmname.acquisitive.network.model.getDisplayedTitle
import dev.jvmname.acquisitive.network.model.score
import dev.jvmname.acquisitive.network.model.url
import dev.jvmname.acquisitive.repo.story.RankedStory
import dev.jvmname.acquisitive.ui.types.Favicon
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import dev.jvmname.acquisitive.util.withMultiFormatArgs
import dev.zacsweers.metro.Inject
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlin.time.Clock

@Inject
class StoryScreenItemConverter() {

    operator fun invoke(ranked: RankedStory, fetchMode: FetchMode): HnScreenItem.Story {
        val story = ranked.item
        require(story !is HnItem.Comment) { "use CommentScreenItemConverter instead for ${story.id}" }

        val time = story.time
            .periodUntil(Clock.System.now(), TimeZone.currentSystemDefault())
            .toAbbreviatedDuration()

        val author = DeferredFormattedString.Resource(R.string.time_author)
            .withFormatArgs(time, story.by.orEmpty())
        val authorInfo = if (story.dead == true) {
            DeferredFormattedString.Resource(R.string.dead)
                .withMultiFormatArgs(author)
        } else {
            author
        }

        val urlHost = story.url?.let(::extractUrlHost)

        return HnScreenItem.Story(
            id = story.id,
            title = story.getDisplayedTitle(),
            isHot = story.score >= fetchMode.hotThreshold,
            score = story.score.toString(),
            rank = "${ranked.rank}.",
            urlHost = urlHost,
            favicon = when {
                urlHost != null -> Favicon.Icon("https://icons.duckduckgo.com/ip3/$urlHost.ico")
                else -> Favicon.Default(Icons.Default.Public)
            },
            numChildren = story.descendants ?: 0,
            author = authorInfo,
            time = time,
            isDeleted = story.deleted ?: false,
            isDead = story.dead ?: false,
            titleSuffix = when {
                story.dead == true -> "☠️"
                story.deleted == true -> "🗑️"
                story is HnItem.Job -> "💼"
                story is HnItem.Poll -> "🗳️"
                else -> null
            }
        )
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
            if (url.isBlank()) ""
            else url.toUri()
                .host
                .orEmpty()
                .removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
    }
}