@file:Suppress("NOTHING_TO_INLINE")

package dev.jvmname.acquisitive.util

import com.squareup.moshi.JsonClass
import dev.jvmname.acquisitive.network.model.ItemId
import kotlinx.serialization.Serializable

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

    private class IIAIterator(private val array: IntArray) : Iterator<ItemId> {
        private var index = 0
        override fun hasNext() = index < array.size
        override fun next() =
            if (index < array.size) ItemId(array[index++]) else throw NoSuchElementException(index.toString())
    }

    override fun contains(element: ItemId) = storage.contains(element.id)

    override fun containsAll(elements: Collection<ItemId>): Boolean {
        return (elements as Collection<*>).all { it is ItemId && storage.contains(it.id) }
    }

    override fun isEmpty(): Boolean = this.storage.isEmpty()

    fun take(n: Int): ItemIdArray {
        require(n >= 0) { "Requested element count $n is less than zero." }
        if (n == 0) return emptyItemIdArray()
        if (n == 1) return itemIdArrayOf(first().id)

        var count = 0
        val storage = storage
        return ItemIdArray(n) { i ->
            ItemId(storage[i])
        }
    }
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
