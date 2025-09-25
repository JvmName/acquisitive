package dev.jvmname.acquisitive.domain

import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.score
import dev.jvmname.acquisitive.network.model.url
import dev.jvmname.acquisitive.repo.story.RankedStory
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import dev.jvmname.acquisitive.ui.types.toScreenItem
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
        return story.toScreenItem(
            isHot = story.score >= fetchMode.hotThreshold,
            suffixIcon = story.prefixIcon(),
            rank = ranked.rank,
            time = story.time.periodUntil(Clock.System.now(), TimeZone.currentSystemDefault())
                .toAbbreviatedDuration(),
            urlHost = story.url?.let(::extractUrlHost)
        ) as HnScreenItem.Story
    }


    private fun HnItem.prefixIcon() = when {
        dead == true -> "☠️"
        deleted == true -> "🗑️"
        this is HnItem.Job -> "💼"
        this is HnItem.Poll -> "🗳️"
        else -> null
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
        internal inline fun extractUrlHost(url: String): String = try {
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