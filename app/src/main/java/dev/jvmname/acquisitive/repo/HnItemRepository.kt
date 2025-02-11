package dev.jvmname.acquisitive.repo

import androidx.compose.ui.util.fastMap
import app.cash.paging.PagingSourceFactory
import dev.jvmname.acquisitive.network.model.FetchMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@Inject
class HnItemRepository(
    private val idStore: ItemIdStore,
    private val itemStore: HnItemStore,
) {

    fun pagingSource(mode: FetchMode): PagingSourceFactory<Int, HnItemEntity> {
        return PagingSourceFactory {
            runBlocking(Dispatchers.IO) { itemStore.pagingSource(idStore.getIds(mode), mode) }
        }
    }

    suspend fun refresh(fetchMode: FetchMode, window: Int): List<HnItemAndRank> =
        withContext(Dispatchers.IO) {
            val ids = idStore.refresh(fetchMode)
            val sliced = ids.slice(0..window)
            return@withContext itemStore.getItemRange(sliced, fetchMode, 0, refresh = true)
                .fastMap(HnItemEntity::toItem)
        }

    suspend fun computeWindow(
        fetchMode: FetchMode,
        start: Int,
        loadSize: Int,
    ): List<HnItemEntity>? = withContext(Dispatchers.IO) {
        val ids = idStore.getIds(fetchMode)
        val indices = ids.indices

        if (start !in indices) return@withContext null // nothing to load
        val end = (start + loadSize).coerceIn(indices)
        val sliced = ids.slice(start..end)
        return@withContext itemStore.getItemRange(sliced, fetchMode, start)
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