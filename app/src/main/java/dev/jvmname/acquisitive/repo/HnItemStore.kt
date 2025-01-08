@file:Suppress("NOTHING_TO_INLINE")

package dev.jvmname.acquisitive.repo

import androidx.compose.runtime.Immutable
import com.mercury.sqkon.db.KeyValueStorage
import com.mercury.sqkon.db.OrderBy
import com.mercury.sqkon.db.OrderDirection
import com.mercury.sqkon.db.Sqkon
import com.mercury.sqkon.db.eq
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.di.AppCrScope
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.ShadedHnItem
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.jvmname.acquisitive.util.fetchAsync
import dev.jvmname.acquisitive.util.retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import logcat.asLog
import logcat.logcat
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse


@Immutable
sealed interface StoryItemKey {
    @[JvmInline Immutable]
    value class Single(val id: ItemId) : StoryItemKey

    @[Poko Immutable]
    class All(val fetchMode: FetchMode, val window: Int) : StoryItemKey
}

sealed interface StoryItemResult<T> {
    @[JvmInline Immutable]
    value class Single<T>(val item: T) : StoryItemResult<T>

    @[Poko Immutable]
    class All<T>(val items: List<T>, val fetchMode: FetchMode) : StoryItemResult<T>
}

private sealed interface NetworkResult<T> {
    @[JvmInline Immutable]
    value class Single<T>(val item: T) : NetworkResult<T>

    @[Poko Immutable]
    class All<T>(
        val full: List<T>,
        val shallow: ItemIdArray,
        val fetchMode: FetchMode,
    ) : NetworkResult<T>
}


private typealias NetworkItem = NetworkResult<HnItem>
private typealias OutputItem = StoryItemResult<ShadedHnItem>


@Inject
class HnItemStore(
    skn: Sqkon,
    private val client: HnClient,
    @AppCrScope scope: CoroutineScope,
) {
    private val storage = skn.keyValueStorage<HnItemEntity>(
        name = HnItemEntity::class.simpleName!!,
        config = KeyValueStorage.Config(dispatcher = Dispatchers.IO)
    )
    private val store = StoreBuilder
        .from(
            fetcher = buildFetcher(),
            sourceOfTruth = buildSoT(),
            converter = buildConverter(),
        )
        .scope(scope)
//        .cachePolicy(
//            MemoryPolicy.MemoryPolicyBuilder<StoryItemKey, OutputItem>()
//                .setMaxSize(20_971_520L) // 20 MB
//                .setExpireAfterWrite(30.minutes)
//                .build()
//        )
        .build()

    suspend fun get(key: StoryItemKey): OutputItem {
        return store.stream(StoreReadRequest.cached(key, refresh = true))
            .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
            .first()
            .requireData()
    }

    fun stream(key: StoryItemKey): Flow<OutputItem> {
        return store.stream(StoreReadRequest.cached(key, refresh = true))
            .mapNotNull { it.dataOrNull() }
    }

    /*
    * below is the setup methods for the store
    */

    private fun buildFetcher() = Fetcher.of<StoryItemKey, NetworkItem> { key ->
        when (key) {
            is StoryItemKey.Single -> {
                NetworkResult.Single(retry(3) { client.getItem(key.id) })
            }

            is StoryItemKey.All -> {
                val itemsIds = try {
                    logcat { "***fetching items for $key" }
                    client.getStories(key.fetchMode)
                } catch (e: Exception) {
                    logcat { "***fetcher internal error: " + e.asLog() }
                    throw e
                }

                val full = itemsIds
                    .take(key.window)
                    .fetchAsync {
                        retry(2) {
                            client.getItem(it) //todo if this fails return the old ID so I don't lose it
                        }
                    }
                val shallow = ItemIdArray(itemsIds.size - key.window) { i ->
                    itemsIds[key.window + i]
                }
                NetworkResult.All(
                    full = full,
                    shallow = shallow,
                    fetchMode = key.fetchMode

                )
            }
        }
    }

    private fun buildSoT(): SourceOfTruth<StoryItemKey, List<HnItemEntity>, OutputItem> {
        return SourceOfTruth.of(
            reader = { key ->
                when (key) {
                    is StoryItemKey.Single -> try {
                        storage.selectByKey(key.toStringId())
                            .map { entity ->
                                entity?.let { StoryItemResult.Single(it.toShadedItem()) }
                            }
                    } catch (e: Exception) {
                        logcat { "***skn error: " + e.asLog() }
                        throw e
                    }

                    is StoryItemKey.All -> try {
                        storage.select(
                            where = HnItemEntity::fetchMode eq key.fetchMode.name,
                            orderBy = listOf(OrderBy(HnItemEntity::index, OrderDirection.ASC))
                        )
                            .map { list ->
                                StoryItemResult.All(
                                    list.map(HnItemEntity::toShadedItem),
                                    key.fetchMode
                                )
                            }
                    } catch (e: Exception) {
                        logcat { "***skn error: " + e.asLog() }
                        throw e
                    }
                }
            },
            writer = { key, item ->
                when (key) {
                    is StoryItemKey.Single -> {
                        try {
                            storage.upsert(key.toStringId(), item.first())
                        } catch (e: Exception) {
                            logcat { "***skn error: " + e.asLog() }
                            throw e
                        }
                    }

                    is StoryItemKey.All -> try {
                        storage.upsertAll(item.associateBy { it.id.toString() })
                    } catch (e: Exception) {
                        logcat { "***skn error: " + e.asLog() }
                        throw e
                    }
                }
            },
            delete = { key ->
                when (key) {
                    is StoryItemKey.Single -> storage.deleteByKey(key.toStringId())
                    is StoryItemKey.All -> storage.delete(HnItemEntity::fetchMode eq key.fetchMode.name)
                }
            },
            deleteAll = storage::deleteAll,
        )
    }

    private fun buildConverter() = Converter.Builder<NetworkItem, List<HnItemEntity>, OutputItem>()
        .fromNetworkToLocal { networkItem ->
            when (networkItem) {
                is NetworkResult.Single -> listOfNotNull(networkItem.item.toEntity())
                is NetworkResult.All -> {
                    val full = networkItem.full
                    val shallow = networkItem.shallow
                    buildList(full.size + shallow.size) {
                        full.forEachIndexed { it, item ->
                            add(item.toEntity(networkItem.fetchMode, index = it))
                        }
                        shallow.forEachIndexed { it, item ->
                            add(item.toEntity(networkItem.fetchMode, index = it + full.size))
                        }
                    }
                }
            }
        }
        .fromOutputToLocal { outputItem ->
            when (outputItem) {
                is StoryItemResult.Single -> listOf(outputItem.item.toEntity(index = -1))
                is StoryItemResult.All -> outputItem.items.mapIndexed { i, item ->
                    item.toEntity(outputItem.fetchMode, i)
                }
            }
        }
        .build()
}

private fun <K : Any, R : Any> Fetcher.Companion.fetcherResult(fetch: suspend (K) -> R): Fetcher<K, R> {
    return ofResult { k ->
        try {
            FetcherResult.Data(fetch(k))
        } catch (e: Exception) {
            logcat { "***fetcher error: " + e.asLog() }
            FetcherResult.Error.Exception(e)
        }
    }
}

private inline fun StoryItemKey.Single.toStringId() = id.id.toString()