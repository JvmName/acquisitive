package dev.jvmname.acquisitive.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class FetchMode {
    @Json(name = "TOP")
    TOP,

    @Json(name = "NEW")
    NEW,

    @Json(name = "ASK")
    ASK,

    @Json(name = "SHOW")
    SHOW,

    @Json(name = "JOBS")
    JOBS,

    @Json(name = "BEST")
    BEST,

}