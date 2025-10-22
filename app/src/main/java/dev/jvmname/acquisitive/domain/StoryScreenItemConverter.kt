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
import dev.jvmname.acquisitive.network.model.score
import dev.jvmname.acquisitive.network.model.url
import dev.jvmname.acquisitive.repo.story.RankedStory
import dev.jvmname.acquisitive.ui.types.Favicon
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import dev.zacsweers.metro.Inject

@Inject
class StoryScreenItemConverter() : ItemConverter() {

    operator fun invoke(ranked: RankedStory, fetchMode: FetchMode): HnScreenItem.Story {
        val story = ranked.item
        require(story !is HnItem.Comment) { "use CommentScreenItemConverter instead for ${story.id}" }

        val time = formatTime(story.time)


        val author = formatAuthor(
            DeferredFormattedString.Resource(R.string.time_author)
                .withFormatArgs(time, story.by.orEmpty()), story.dead
        )
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
            author = author,
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

private fun HnItem.getDisplayedTitle() = when (this) {
    is HnItem.Comment -> text.orEmpty()
    is HnItem.Job -> title
    is HnItem.Poll -> title
    is HnItem.PollOption -> text.orEmpty()
    is HnItem.Story -> title
}