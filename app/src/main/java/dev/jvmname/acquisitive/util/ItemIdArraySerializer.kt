package dev.jvmname.acquisitive.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
object ItemIdArraySerializer : KSerializer<ItemIdArray> {
    private val delegate = IntArraySerializer()


    override val descriptor: SerialDescriptor =
        SerialDescriptor("acq.ItemIdArraySerializer", delegate.descriptor)

    override fun serialize(encoder: Encoder, value: ItemIdArray) {
        encoder.encodeSerializableValue(delegate, value.storage)
    }

    override fun deserialize(decoder: Decoder): ItemIdArray {
        return ItemIdArray(decoder.decodeSerializableValue(delegate))
    }

}