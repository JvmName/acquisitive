package dev.jvmname.acquisitive.network.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonClass
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import kotlinx.datetime.Instant
import kotlinx.parcelize.Parcelize

@Immutable
sealed interface ShadedHnItem {
    @JvmInline
    value class Shallow(val item: ItemId) : ShadedHnItem

    @JvmInline
    value class Full(val item: HnItem) : ShadedHnItem
}

@[JvmInline Parcelize Immutable JsonClass(generateAdapter = false)]
value class ItemId(val id: Int) : Parcelable

@[Immutable JsonClass(generateAdapter = true, generator = "sealed:type")]
sealed interface HnItem {
    abstract val id: ItemId
    abstract val by: String?
    abstract val time: Instant
    abstract val dead: Boolean?
    abstract val deleted: Boolean?
    abstract val kids: ItemIdArray?

    @[Poko TypeLabel("story") JsonClass(generateAdapter = true)]
    class Story(
        override val id: ItemId,
        override val by: String?,
        override val time: Instant,
        override val dead: Boolean?,
        override val deleted: Boolean?,
        override val kids: ItemIdArray?,
        val title: String,
        val url: String?,
        val score: Int,
        val descendants: Int?,
        val text: String?,
    ) : HnItem

    @[Poko TypeLabel("comment") JsonClass(generateAdapter = true)]
    class Comment(
        override val id: ItemId,
        override val by: String?,
        override val time: Instant,
        override val dead: Boolean?,
        override val deleted: Boolean?,
        override val kids: ItemIdArray?,
        val text: String?,
        val parent: Int,
    ) : HnItem

    @[Poko TypeLabel("job") JsonClass(generateAdapter = true)]
    class Job(
        override val id: ItemId,
        override val by: String?,
        override val time: Instant,
        override val dead: Boolean?,
        override val deleted: Boolean?,
        override val kids: ItemIdArray?,
        val title: String,
        val text: String?,
        val url: String?,
        val score: Int,
    ) : HnItem

    @[Poko TypeLabel("poll") JsonClass(generateAdapter = true)]
    class Poll(
        override val id: ItemId,
        override val by: String?,
        override val time: Instant,
        override val dead: Boolean?,
        override val deleted: Boolean?,
        override val kids: ItemIdArray?,
        val title: String,
        val text: String?,
        val parts: ItemIdArray,
        val score: Int,
        val descendants: Int?,
    ) : HnItem

    @[Poko TypeLabel("pollopt") JsonClass(generateAdapter = true)]
    class PollOption(
        override val id: ItemId,
        override val by: String?,
        override val time: Instant,
        override val dead: Boolean?,
        override val deleted: Boolean?,
        override val kids: ItemIdArray?,
        val poll: ItemId,
        val text: String?,
        val score: Int,
    ) : HnItem
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

fun ItemId.shaded() = ShadedHnItem.Shallow(this)
fun HnItem.shaded() = ShadedHnItem.Full(this)
