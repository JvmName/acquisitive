package dev.jvmname.acquisitive.network.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonAdapter.Factory
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.UserId

class IdAdapter<T>(
    private val reader: (Int) -> T,
    private val writer: (T) -> Int,
) : JsonAdapter<T>() {
    override fun fromJson(reader: JsonReader): T? = reader(reader.nextInt())

    override fun toJson(writer: JsonWriter, value: T?) {
        if (value == null) writer.nullValue()
        else writer.value(writer(value))
    }

    companion object {
        fun create(): Factory {
            return Factory { type, _, _ ->
                when (type) {
                    ItemId::class.java -> IdAdapter(::ItemId, ItemId::id)
                    UserId::class.java -> IdAdapter(::UserId, UserId::id)
                    else -> null
                }
            }
        }
    }
}