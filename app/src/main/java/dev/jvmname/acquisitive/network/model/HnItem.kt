package dev.jvmname.acquisitive.network.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonClass
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import kotlinx.datetime.Instant
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@[JvmInline Parcelize Immutable Serializable JsonClass(generateAdapter = false)]
value class ItemId(val id: Int) : Parcelable {
    override fun toString(): String = id.toString()
}

@[Immutable Serializable JsonClass(generateAdapter = true, generator = "sealed:type")]
sealed interface HnItem {
    val id: ItemId
    val by: String?
    val time: Instant
    val dead: Boolean?
    val deleted: Boolean?
    val kids: ItemIdArray?

    @[Poko Serializable TypeLabel("story") JsonClass(generateAdapter = true)]
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

    @[Poko Serializable TypeLabel("comment") JsonClass(generateAdapter = true)]
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

    @[Poko Serializable TypeLabel("job") JsonClass(generateAdapter = true)]
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

    @[Poko Serializable TypeLabel("poll") JsonClass(generateAdapter = true)]
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