package dev.jvmname.acquisitive.network.model

import com.squareup.moshi.JsonClass
import dev.drewhamilton.poko.Poko
import kotlinx.datetime.Instant


@[JvmInline JsonClass(generateAdapter = false)]
value class UserId(val id: Int)

@[Poko JsonClass(generateAdapter = true)]
class User(
    val id: UserId,
    val created: Instant,
    val karma: Int,
    val about: String? = null,
    val submitted: List<ItemId>? = null,
)

