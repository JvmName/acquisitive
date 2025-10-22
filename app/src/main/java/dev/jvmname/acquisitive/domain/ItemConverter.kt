package dev.jvmname.acquisitive.domain

import com.backbase.deferredresources.DeferredFormattedString
import com.backbase.deferredresources.DeferredText
import dev.jvmname.acquisitive.R
import dev.jvmname.acquisitive.util.withMultiFormatArgs
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import org.jetbrains.annotations.VisibleForTesting
import kotlin.time.Clock
import kotlin.time.Instant

abstract class ItemConverter {
    @VisibleForTesting
    internal fun formatTime(time: Instant): String {
        return time.periodUntil(Clock.System.now(), TimeZone.currentSystemDefault())
            .toAbbreviatedDuration()
    }

    @VisibleForTesting
    internal fun formatAuthor(author: String?, dead: Boolean?): DeferredText {
        return formatAuthor(DeferredText.Constant(author.orEmpty()), dead)
    }

    @VisibleForTesting
    internal fun formatAuthor(author: DeferredText, dead: Boolean?): DeferredText = when (dead) {
        true -> DeferredFormattedString.Resource(R.string.dead).withMultiFormatArgs(author)

        else -> author
    }

    private fun DateTimePeriod.toAbbreviatedDuration(): String = when {
        years > 0 -> "${years}y"
        months > 0 -> "${months}mo"
        days >= 7 -> "${days / 7}w"
        days > 0 -> "${days}d"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        seconds > 10 -> "${seconds}s"
        else -> "<1s"
    }

}