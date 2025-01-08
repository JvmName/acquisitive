package dev.jvmname.acquisitive.repo

import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.ShadedHnItem
import dev.jvmname.acquisitive.util.ItemIdArray
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@[Poko Serializable]
class HnItemEntity(
    val id: Int,
    val type: String?,  // "story", "comment", "job", "poll", "pollopt"
    val author: String?,
    val time: Instant?,
    val dead: Boolean?,
    val deleted: Boolean?,
    val kids: ItemIdArray?,
    val title: String?,
    val url: String?,
    val text: String?,
    val score: Int?,
    val descendants: Int?,
    val parent: Int?,
    val poll: Int?,
    val parts: ItemIdArray?,
    val fetchMode: FetchMode?,
    val index: Int,
)

fun ShadedHnItem.toEntity(fetchMode: FetchMode? = null, index: Int): HnItemEntity = when (this) {
    is ShadedHnItem.Shallow -> item.toEntity(fetchMode, index)
    is ShadedHnItem.Full -> item.toEntity(fetchMode, index)
}


fun HnItemEntity.toShadedItem(): ShadedHnItem {
    if (type == null || time == null) return ShadedHnItem.Shallow(ItemId(id))
    return ShadedHnItem.Full(
        when (type) {
            "story" -> HnItem.Story(
                id = ItemId(id),
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
                id = ItemId(id),
                by = author,
                time = time,
                dead = dead,
                deleted = deleted,
                kids = kids,
                text = text,
                parent = parent ?: error("Comment must have parent")
            )

            "job" -> HnItem.Job(
                id = ItemId(id),
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
                id = ItemId(id),
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
                id = ItemId(id),
                by = author,
                time = time,
                dead = dead,
                deleted = deleted,
                kids = kids,
                poll = ItemId(poll ?: error("PollOption must have poll")),
                text = text,
                score = score ?: 0,
            )

            else -> error("Unknown item type: $type")
        }
    )
}

fun ItemId.toEntity(fetchMode: FetchMode? = null, index: Int): HnItemEntity {
    return HnItemEntity(
        id = this.id,
        type = null,
        author = null,
        time = null,
        dead = null,
        deleted = null,
        kids = null,
        title = null,
        url = null,
        text = null,
        score = null,
        descendants = null,
        parent = null,
        poll = null,
        parts = null,
        fetchMode = fetchMode,
        index = index
    )
}

fun HnItem.toEntity(fetchMode: FetchMode? = null, index:Int = -1): HnItemEntity {
    val type = when (this) {
        is HnItem.Story -> "story"
        is HnItem.Comment -> "comment"
        is HnItem.Job -> "job"
        is HnItem.Poll -> "poll"
        is HnItem.PollOption -> "pollopt"
    }

    return HnItemEntity(
        id = id.id,
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
            is HnItem.PollOption -> poll.id
            else -> null
        },
        parts = when (this) {
            is HnItem.Poll -> parts
            else -> null
        },
        fetchMode = fetchMode,
        index = index
    )
}