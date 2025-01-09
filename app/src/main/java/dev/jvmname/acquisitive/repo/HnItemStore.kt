@file:Suppress("NOTHING_TO_INLINE")

package dev.jvmname.acquisitive.repo

import android.util.SparseArray
import com.mercury.sqkon.db.KeyValueStorage
import com.mercury.sqkon.db.Sqkon
import dev.jvmname.acquisitive.di.AppCrScope
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.ShadedHnItem
import dev.jvmname.acquisitive.network.model.id
import dev.jvmname.acquisitive.network.model.shaded
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.jvmname.acquisitive.util.fetchAsync
import dev.jvmname.acquisitive.util.retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject

@[Serializable JvmInline]
private value class ShadedItemList(val list: List<ShadedHnItem>)

@Inject
class HnItemStore(
    skn: Sqkon,
    private val client: HnClient,
    @AppCrScope private val scope: CoroutineScope,
) {
    private val storage = skn.keyValueStorage<ShadedItemList>(
        name = ShadedItemList::class.simpleName!!,
        config = KeyValueStorage.Config(dispatcher = Dispatchers.IO)
    )

    suspend fun getItem(mode: FetchMode, itemId: ItemId): ShadedHnItem.Full {
        val storedList = coroutineScope {
            storage.selectByKey(mode.value)
                .firstOrNull()
                ?.list.orEmpty()
        }
        val storedIndex = storedList.indexOfFirst { it.id == itemId }
        val shouldCache = storedIndex >= 0
        val networkItem = withContext(Dispatchers.IO) { client.getItem(itemId) }

        return networkItem.shaded().also {
            if (shouldCache) {
                storedList
                    .toMutableList()
                    .apply {
                        set(storedIndex, ShadedHnItem.Full(networkItem))
                    }
                scope.launch(Dispatchers.IO) {
                    storage.update(mode.value, ShadedItemList(storedList))
                }
            }
        }
    }

    fun stream(mode: FetchMode, window: Int): Flow<List<ShadedHnItem>> {
        return storage.selectByKey(mode.value)
            .map { it?.list.orEmpty() }
            .onStart { emit(getNetworkItems(mode, window)) }
    }


    private suspend fun getNetworkItems(mode: FetchMode, window: Int): List<ShadedHnItem> {
        val fullThenShallows = fetchNetworkItems(mode, window)

        val storedList = coroutineScope {
            storage.selectByKey(mode.value)
                .map { it?.list }
                .firstOrNull()
                .orEmpty()
        }
        if (storedList.isEmpty() || fullThenShallows.size > storedList.size) {
            scope.launch(Dispatchers.IO) {
                storage.update(mode.value, ShadedItemList(fullThenShallows))
            }
            return fullThenShallows
        }
        val mapping = buildMapping(storedList)

        //copy any Full items into the FullThenShallows list
        val fullyUpdated = fullThenShallows.map { item ->
            if (item is ShadedHnItem.Shallow) {
                when (val fromMapping = mapping[item.id.id]) {
                    null, is ShadedHnItem.Shallow -> item
                    else -> fromMapping
                }
            } else {
                item
            }
        }
        scope.launch(Dispatchers.IO) {
            storage.update(mode.value, ShadedItemList(fullyUpdated))
        }
        return fullyUpdated
    }

    private suspend fun fetchNetworkItems(mode: FetchMode, window: Int): List<ShadedHnItem> =
        withContext(Dispatchers.IO) {
            val ids = client.getStories(mode)
            val fulls = ids.take(window).fetchAsync { retry(3) { client.getItem(it) } }
            val shallows = ItemIdArray(ids.size - window) { i -> ids[window + i] }

            //equivalent to fulls.map(::Full) + shallows.map(::Shallow), but with fewer allocations
            buildList(ids.size) {
                fulls.forEach { this.add(ShadedHnItem.Full(it)) }
                shallows.forEach { this.add(ShadedHnItem.Shallow(it)) }
            }
        }

    private fun buildMapping(storedList: List<ShadedHnItem>): SparseArray<ShadedHnItem> {
        val map = SparseArray<ShadedHnItem>(storedList.size)
        storedList.forEach { item ->
            map.append(item.id.id, item)
        }
        return map
    }

}
