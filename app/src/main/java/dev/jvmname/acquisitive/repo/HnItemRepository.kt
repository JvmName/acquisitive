package dev.jvmname.acquisitive.repo

import androidx.compose.ui.util.fastMap
import app.cash.paging.PagingSourceFactory
import dev.jvmname.acquisitive.network.model.FetchMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Inject

@Inject
class HnItemRepository(
    private val idStore: ItemIdStore,
    private val itemStore: HnItemStore,
) {

    fun pagingSource(mode: FetchMode): PagingSourceFactory<Int, HnItemEntity> {
        return PagingSourceFactory {
            runBlocking { itemStore.pagingSource(idStore.getIds(mode), mode) }
        }
    }

    suspend fun refresh(fetchMode: FetchMode, window: Int): List<HnItemAndRank> {
        val ids = idStore.refresh(fetchMode)
        val sliced = ids.slice(0..window)
        return itemStore.getItemRange(sliced, fetchMode, 0, refresh = true)
            .fastMap(HnItemEntity::toItem)
    }

    suspend fun computeWindow(
        fetchMode: FetchMode,
        start: Int,
        loadSize: Int,
    ): List<HnItemEntity>? {
        val ids = idStore.getIds(fetchMode)
        val indices = ids.indices

        if (start !in indices) return null // nothing to load
        val end = (start + loadSize).coerceIn(indices)
        val sliced = ids.slice(start..end)
        return itemStore.getItemRange(sliced, fetchMode, start)
    }

    fun stream(fetchMode: FetchMode): Flow<List<HnItemEntity>> {
        return flow {
            val ids = idStore.getIds(fetchMode)
            emitAll(itemStore.stream(fetchMode, ids))
        }
    }

    suspend fun clearExpired() {
        idStore.clearExpired()
        itemStore.clearExpired()
    }
}