package dev.jvmname.acquisitive.network.model

enum class FetchMode(val value: String) {
    TOP("top"),
    NEW("new"),
    ASK("ask"),
    SHOW("show"),
    JOBS("jobs"),
    BEST("best"),
}