@file:Suppress("NOTHING_TO_INLINE")

package dev.jvmname.acquisitive.util

import dev.jvmname.acquisitive.network.model.ItemId
import kotlinx.serialization.Serializable

@[JvmInline Serializable(with = ItemIdArraySerializer::class)]
value class ItemIdArray
@PublishedApi internal constructor(@PublishedApi internal val storage: IntArray) :
    Collection<ItemId> {

    /** Creates a new array of the specified [size], with all elements initialized to zero. */
    constructor(size: Int) : this(IntArray(size))

    /**
     * Returns the array element at the given [index]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    operator fun get(index: Int): ItemId = ItemId(storage[index])

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    operator fun set(index: Int, value: ItemId) {
        storage[index] = value.id
    }

    /** Returns the number of elements in the array. */
    override val size: Int get() = storage.size

    /** Creates an iterator over the elements of the array. */
    override operator fun iterator(): kotlin.collections.Iterator<ItemId> = Iterator(storage)

    private class Iterator(private val array: IntArray) : kotlin.collections.Iterator<ItemId> {
        private var index = 0
        override fun hasNext() = index < array.size
        override fun next() =
            if (index < array.size) ItemId(array[index++]) else throw NoSuchElementException(index.toString())
    }

    override fun contains(element: ItemId): Boolean {
        return storage.contains(element.id)
    }

    override fun containsAll(elements: Collection<ItemId>): Boolean {
        return (elements as Collection<*>).all { it is ItemId && storage.contains(it.id) }
    }

    override fun isEmpty(): Boolean = this.storage.isEmpty()
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

inline fun ItemIdArrayOf(vararg elements: Int): ItemIdArray =
    ItemIdArray(elements.size) { ItemId(elements[it]) }

inline fun emptyItemIdArray() = ItemIdArray(size = 0)
