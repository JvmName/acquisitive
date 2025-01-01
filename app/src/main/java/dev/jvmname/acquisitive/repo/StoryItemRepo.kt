@file:Suppress("NOTHING_TO_INLINE")

package dev.jvmname.acquisitive.repo

import com.mercury.sqkon.db.Sqkon
import com.mercury.sqkon.db.eq
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.di.AppCrScope
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.util.fetchAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.impl.extensions.get
import kotlin.time.Duration.Companion.minutes

private sealed interface StoryItemKey {
    @JvmInline
    value class Single(val id: ItemId) : StoryItemKey

    @JvmInline
    value class All(val fetchMode: FetchMode) : StoryItemKey
}

private sealed interface StoryItemResult<T> {
    @JvmInline
    value class Single<T>(val item: T) : StoryItemResult<T>

    @Poko
    class All<T>(val items: List<T>, val fetchMode: FetchMode) : StoryItemResult<T>
}

private typealias NetworkItem = StoryItemResult<HnItem>

@Inject
class StoryItemRepo(
    skn: Sqkon,
    private val client: HnClient,
    @AppCrScope scope: CoroutineScope,
) {
    private val storage = skn.keyValueStorage<HnItemEntity>("storyItem")
    private val store = StoreBuilder
        .from(
            fetcher = buildFetcher(),
            sourceOfTruth = buildSoT(),
            converter = buildConverter(),
        )
        .scope(scope)
        .cachePolicy(
            MemoryPolicy.MemoryPolicyBuilder<StoryItemKey, NetworkItem>()
                .setMaxSize(20_971_520L) // 20 MB
                .setExpireAfterWrite(30.minutes)
                .build()
        )
        .build()

    suspend fun getStory(id: ItemId): HnItem {
        val result = store.get(StoryItemKey.Single(id))
        result as StoryItemResult.Single
        return result.item
    }

    fun observeStories(fetchMode: FetchMode): Flow<List<HnItem>> {
        val key = StoryItemKey.All(fetchMode)
        return store.stream(StoreReadRequest.cached(key, refresh = true))
            .mapNotNull { it.dataOrNull() as? StoryItemResult.All }
            .map { it.items }
    }


    private fun buildFetcher() =
        Fetcher.fetcherResult<StoryItemKey, NetworkItem> { key ->
            when (key) {
                is StoryItemKey.Single -> {
                    StoryItemResult.Single(client.getItem(key.id))
                }

                is StoryItemKey.All -> {
                    val items = client.getStories(key.fetchMode)
                        .fetchAsync { client.getItem(it) }
                    StoryItemResult.All(items, key.fetchMode)
                }
            }
        }

    private fun buildSoT(): SourceOfTruth<StoryItemKey, List<HnItemEntity>, NetworkItem> {
        return SourceOfTruth.of(
            reader = { key ->
                when (key) {
                    is StoryItemKey.Single -> storage.selectByKey(key.toStringId())
                        .map { entity ->
                            entity?.let { StoryItemResult.Single(it.toHnItem()) }
                        }

                    is StoryItemKey.All -> storage.select(HnItemEntity::fetchMode eq key.fetchMode.name)
                        .map { list ->
                            StoryItemResult.All(list.map(HnItemEntity::toHnItem), key.fetchMode)
                        }
                }
            },
            writer = { key, item ->
                when (key) {
                    is StoryItemKey.Single -> storage.insert(key.toStringId(), item.first())
                    is StoryItemKey.All -> storage.insertAll(item.associateBy { it.id.toString() })
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

    private fun buildConverter(): Converter<NetworkItem, List<HnItemEntity>, NetworkItem> {
        val networkToEntity: (network: NetworkItem) -> List<HnItemEntity> = { network ->
            when (network) {
                is StoryItemResult.Single -> listOfNotNull(network.item?.toEntity(null))
                is StoryItemResult.All -> network.items.map { it.toEntity(network.fetchMode) }
            }
        }
        return Converter.Builder<NetworkItem, List<HnItemEntity>, NetworkItem>()
            .fromNetworkToLocal(networkToEntity)
            .fromOutputToLocal(networkToEntity)
            .build()
    }
}

private fun <K : Any, R : Any> Fetcher.Companion.fetcherResult(fetch: suspend (K) -> R): Fetcher<K, R> {
    return ofResult { k ->
        try {
            FetcherResult.Data(fetch(k))
        } catch (e: Exception) {
            FetcherResult.Error.Exception(e)
        }
    }
}

private inline fun StoryItemKey.Single.toStringId() = id.id.toString()