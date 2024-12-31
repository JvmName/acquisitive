package dev.jvmname.acquisitive.network.model

import com.squareup.moshi.JsonClass
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import kotlinx.datetime.Instant


@JvmInline
value class ItemId(val id: Int)

@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed class HnItem(val id: ItemId) {
    abstract val by: String?
    abstract val time: Instant
    abstract val dead: Boolean?
    abstract val deleted: Boolean?
    abstract val kids: ItemIdArray?

    @[Poko TypeLabel("story") JsonClass(generateAdapter = true)]
    class Story(
        id: ItemId,
        override val by: String?,
        override val time: Instant,
        override val dead: Boolean? = null,
        override val deleted: Boolean? = null,
        override val kids: ItemIdArray? = null,
        val title: String,
        val url: String?,
        val score: Int,
        val descendants: Int?,
        val text: String?,
    ) : HnItem(id)

    @[Poko TypeLabel("comment") JsonClass(generateAdapter = true)]
    class Comment(
        id: ItemId,
        override val by: String?,
        override val time: Instant,
        override val dead: Boolean? = null,
        override val deleted: Boolean? = null,
        override val kids: ItemIdArray? = null,
        val text: String?,
        val parent: Int,
    ) : HnItem(id)

    @[Poko TypeLabel("job") JsonClass(generateAdapter = true)]
    class Job(
        id: ItemId,
        override val by: String?,
        override val time: Instant,
        override val dead: Boolean? = null,
        override val deleted: Boolean? = null,
        override val kids: ItemIdArray? = null,
        val title: String,
        val text: String?,
        val url: String?,
        val score: Int,
    ) : HnItem(id)

    @[Poko TypeLabel("poll") JsonClass(generateAdapter = true)]
    class Poll(
        id: ItemId,
        override val by: String?,
        override val time: Instant,
        override val dead: Boolean? = null,
        override val deleted: Boolean? = null,
        override val kids: ItemIdArray? = null,
        val title: String,
        val text: String?,
        val parts: ItemIdArray,
        val score: Int,
        val descendants: Int?,
    ) : HnItem(id)

    @[Poko TypeLabel("pollopt") JsonClass(generateAdapter = true)]
    class PollOption(
        id: ItemId,
        override val by: String?,
        override val time: Instant,
        override val dead: Boolean? = null,
        override val deleted: Boolean? = null,
        override val kids: ItemIdArray? = null,
        val poll: ItemId,
        val text: String?,
        val score: Int,
    ) : HnItem(id)
}

val HnItem.score: Int
    get() = when (this) {
        is HnItem.Comment -> 0
        is HnItem.Job -> score
        is HnItem.Poll -> score
        is HnItem.PollOption -> score
        is HnItem.Story -> score
    }

fun HnItem.getDisplayedTitle() = when (this) {
    is HnItem.Comment -> text.orEmpty()
    is HnItem.Job -> title
    is HnItem.Poll -> title
    is HnItem.PollOption -> text.orEmpty()
    is HnItem.Story -> title
}

fun HnItem.copy(
    id: ItemId = this.id,
    by: String? = this.by,
    time: Instant = this.time,
    dead: Boolean? = this.dead,
    deleted: Boolean? = this.deleted,
    kids: ItemIdArray? = this.kids,
) = when (this) {
    is HnItem.Story -> HnItem.Story(
        id = id,
        by = by,
        time = time,
        dead = dead,
        deleted = deleted,
        kids = kids,
        title = title,
        url = url,
        score = score,
        descendants = descendants,
        text = text
    )

    is HnItem.Comment -> HnItem.Comment(
        id = id,
        by = by,
        time = time,
        dead = dead,
        deleted = deleted,
        kids = kids,
        text = text,
        parent = parent
    )

    is HnItem.Job -> HnItem.Job(
        id = id,
        by = by,
        time = time,
        dead = dead,
        deleted = deleted,
        kids = kids,
        title = title,
        text = text,
        url = url,
        score = score
    )

    is HnItem.Poll -> HnItem.Poll(
        id = id,
        by = by,
        time = time,
        dead = dead,
        deleted = deleted,
        kids = kids,
        title = title,
        text = text,
        parts = parts,
        score = score,
        descendants = descendants
    )

    is HnItem.PollOption -> HnItem.PollOption(
        id = id,
        by = by,
        time = time,
        dead = dead,
        deleted = deleted,
        kids = kids,
        poll = poll,
        text = text,
        score = score
    )
}