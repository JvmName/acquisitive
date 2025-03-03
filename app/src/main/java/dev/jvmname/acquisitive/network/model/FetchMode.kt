package dev.jvmname.acquisitive.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FetchMode {
    @SerialName("TOP")
    TOP,

    @SerialName("NEW")
    NEW,

    @SerialName("ASK")
    ASK,

    @SerialName("SHOW")
    SHOW,

    @SerialName("JOBS")
    JOBS,

    @SerialName("BEST")
    BEST,

}