package dev.jvmname.acquisitive.network.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import kotlin.time.Instant

object InstantAdapter : JsonAdapter<Instant>() {
    override fun fromJson(reader: JsonReader): Instant {
        return Instant.fromEpochSeconds(reader.nextLong())
    }

    override fun toJson(writer: JsonWriter, value: Instant?) {
        val write = value?.epochSeconds
        if (write == null) writer.nullValue()
        else writer.value(write)
    }
}