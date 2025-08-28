package dev.jvmname.acquisitive.repo

import dev.jvmname.acquisitive.db.HnItemEntity
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem

data class HnRankedItem(val item: HnItem, val rank: Int)

fun HnItemEntity.toItem(): HnRankedItem {
    return HnRankedItem(
        item = when (type) {
            "story" -> HnItem.Story(
                id = id,
                by = author,
                time = time,
                dead = dead,
                deleted = deleted,
                kids = kids,
                title = title ?: error("Story must have title"),
                url = url,
                score = score ?: 0,
                descendants = descendants,
                text = text
            )

            "comment" -> HnItem.Comment(
                id = id,
                by = author,
                time = time,
                dead = dead,
                deleted = deleted,
                kids = kids,
                text = text,
                parent = parent ?: error("Comment must have parent"),

                )

            "job" -> HnItem.Job(
                id = id,
                by = author,
                time = time,
                dead = dead,
                deleted = deleted,
                kids = kids,
                title = title ?: error("Job must have title"),
                text = text,
                url = url,
                score = score ?: 0,
            )

            "poll" -> HnItem.Poll(
                id = id,
                by = author,
                time = time,
                dead = dead,
                deleted = deleted,
                kids = kids,
                title = title ?: error("Poll must have title"),
                text = text,
                parts = parts ?: error("Poll must have parts"),
                score = score ?: 0,
                descendants = descendants

            )

            "pollopt" -> HnItem.PollOption(
                id = id,
                by = author,
                time = time,
                dead = dead,
                deleted = deleted,
                kids = kids,
                poll = poll ?: error("PollOption must have poll"),
                text = text,
                score = score ?: 0,
            )

            else -> error("Unknown item type: $type")
        },
        rank = rank,
    )
}


fun HnItem.toEntity(index: Int, mode: FetchMode): HnItemEntity {
    val type = when (this) {
        is HnItem.Story -> "story"
        is HnItem.Comment -> "comment"
        is HnItem.Job -> "job"
        is HnItem.Poll -> "poll"
        is HnItem.PollOption -> "pollopt"
    }

    return HnItemEntity(
        id = id,
        rank = index + 1,
        fetchMode = mode,
        type = type,
        author = by,
        time = time,
        dead = dead,
        deleted = deleted,
        kids = kids,
        title = when (this) {
            is HnItem.Story -> title
            is HnItem.Job -> title
            is HnItem.Poll -> title
            else -> null
        },
        url = when (this) {
            is HnItem.Story -> url
            is HnItem.Job -> url
            else -> null
        },
        text = when (this) {
            is HnItem.Story -> text
            is HnItem.Comment -> text
            is HnItem.Job -> text
            is HnItem.Poll -> text
            is HnItem.PollOption -> text
        },
        score = when (this) {
            is HnItem.Story -> score
            is HnItem.Job -> score
            is HnItem.Poll -> score
            is HnItem.PollOption -> score
            else -> null
        },
        descendants = when (this) {
            is HnItem.Story -> descendants
            is HnItem.Poll -> descendants
            else -> null
        },
        parent = when (this) {
            is HnItem.Comment -> parent
            else -> null
        },
        poll = when (this) {
            is HnItem.PollOption -> poll
            else -> null
        },
        parts = when (this) {
            is HnItem.Poll -> parts
            else -> null
        },
    )
}