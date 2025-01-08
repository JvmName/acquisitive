package dev.jvmname.acquisitive.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import dev.jvmname.acquisitive.util.ItemIdArray
import java.lang.reflect.Type

object ItemIdArrayAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: MutableSet<out Annotation>,
        moshi: Moshi,
    ): JsonAdapter<ItemIdArray>? {
        if (type != ItemIdArray::class.java) return null

        val delegate = moshi.adapter<IntArray>()
        return object : JsonAdapter<ItemIdArray>() {
            override fun fromJson(reader: JsonReader): ItemIdArray? {
                return delegate.fromJson(reader)?.let(::ItemIdArray)
            }

            override fun toJson(writer: JsonWriter, value: ItemIdArray?) {
                delegate.toJson(writer, value?.storage)
            }
        }
    }

}