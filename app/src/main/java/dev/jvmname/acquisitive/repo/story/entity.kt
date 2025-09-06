package dev.jvmname.acquisitive.repo.story

import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.db.StoryEntity
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem

@Poko
class RankedStory(val item: HnItem, val rank: Int)

enum class ItemType { STORY, COMMENT, JOB, POLL, POLLOPTION, }

fun StoryEntity.toStory(): RankedStory {
    return RankedStory(
        item = when (type) {
            ItemType.STORY -> HnItem.Story(
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

            ItemType.COMMENT -> HnItem.Comment(
                id = id,
                by = author,
                time = time,
                dead = dead,
                deleted = deleted,
                kids = kids,
                text = text,
                parent = parent ?: error("Comment must have parent"),

                )

            ItemType.JOB -> HnItem.Job(
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

            ItemType.POLL -> HnItem.Poll(
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

            ItemType.POLLOPTION -> HnItem.PollOption(
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
        },
        rank = rank,
    )
}


fun HnItem.toEntity(index: Int, mode: FetchMode): StoryEntity = StoryEntity(
    id = id,
    rank = index + 1,
    fetchMode = mode,
    type = when (this) {
        is HnItem.Story -> ItemType.STORY
        is HnItem.Comment -> ItemType.COMMENT
        is HnItem.Job -> ItemType.JOB
        is HnItem.Poll -> ItemType.POLL
        is HnItem.PollOption -> ItemType.POLLOPTION
    },
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