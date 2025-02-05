package dev.jvmname.acquisitive.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FetchMode(val value: String) {
    @SerialName("top")
    TOP("top"),

    @SerialName("new")
    NEW("new"),

    @SerialName("ask")
    ASK("ask"),

    @SerialName("show")
    SHOW("show"),

    @SerialName("jobs")
    JOBS("jobs"),

    @SerialName("best")
    BEST("best"),

}