@file:Suppress("NOTHING_TO_INLINE")

package dev.jvmname.acquisitive.util

import com.squareup.moshi.JsonClass
import dev.jvmname.acquisitive.network.model.ItemId
import kotlinx.serialization.Serializable
import kotlin.contracts.contract

@[JvmInline Serializable(with = ItemIdArraySerializer::class) JsonClass(generateAdapter = false)]
value class ItemIdArray
@PublishedApi internal constructor(@PublishedApi internal val storage: IntArray) :
    Collection<ItemId> {

    constructor(size: Int) : this(IntArray(size))

    operator fun get(index: Int): ItemId = ItemId(storage[index])
    operator fun set(index: Int, value: ItemId) {
        storage[index] = value.id
    }

    override val size: Int get() = storage.size

    override operator fun iterator(): Iterator<ItemId> = IIAIterator(storage)

    private class IIAIterator(array: IntArray) : Iterator<ItemId> {
        private val delegate = array.iterator()
        override fun hasNext() = delegate.hasNext()
        override fun next() = ItemId(delegate.next())
    }

    override fun contains(element: ItemId) = storage.contains(element.id)

    override fun containsAll(elements: Collection<ItemId>): Boolean {
        return elements.all { it.id in storage }
    }

    override fun isEmpty(): Boolean = storage.isEmpty()

    fun take(n: Int): ItemIdArray {
        require(n >= 0) { "Requested element count $n is less than zero." }
        if (n == 0) return emptyItemIdArray()
        if (n == 1) return itemIdArrayOf(first().id)

        val _storage = storage
        return ItemIdArray(n) { i ->
            ItemId(_storage[i])
        }
    }

    fun slice(indices: IntRange): ItemIdArray {
        if (indices.isEmpty()) return emptyItemIdArray()
        return ItemIdArray(storage.sliceArray(indices))
    }


    inline fun forEach(action: (ItemId) -> Unit) {
        contract { callsInPlace(action) }
        for (index in indices) {
            action(get(index))
        }
    }

    val indices: IntRange get() = storage.indices
}

/**
 * Creates a new array of the specified [size], where each element is calculated by calling the specified
 * [init] function.
 *
 * The function [init] is called for each array element sequentially starting from the first one.
 * It should return the value for an array element given its index.
 */
inline fun ItemIdArray(size: Int, init: (Int) -> ItemId): ItemIdArray {
    return ItemIdArray(IntArray(size) { index -> init(index).id })
}

inline fun itemIdArrayOf(vararg elements: Int): ItemIdArray =
    ItemIdArray(elements.size) { ItemId(elements[it]) }

inline fun emptyItemIdArray() = ItemIdArray(size = 0)