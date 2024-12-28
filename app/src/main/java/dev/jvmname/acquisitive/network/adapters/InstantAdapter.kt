package dev.jvmname.acquisitive.network.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import kotlinx.datetime.Instant
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@ContributesBinding(AppScope::class, boundType = JsonAdapter::class, multibinding = true)
class InstantAdapter @Inject constructor() : JsonAdapter<Instant>() {
    override fun fromJson(reader: JsonReader): Instant {
        return Instant.fromEpochSeconds(reader.nextLong())
    }

    override fun toJson(writer: JsonWriter, value: Instant?) {
        val write = value?.epochSeconds
        if (write == null) writer.nullValue()
        else writer.value(write)
    }
}