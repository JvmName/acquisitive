package dev.jvmname.acquisitive.repo

import androidx.compose.ui.util.fastMap
import androidx.paging.PagingSource
import dev.jvmname.acquisitive.db.HnItemEntity
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.time.Instant

@Inject
class HnItemRepository(
    private val idStore: ItemIdStore,
    private val itemStore: HnItemStore,
) {

    fun pagingSource(mode: FetchMode): PagingSource<ItemId, HnRankedItem> {
        return itemStore.pagingSource(mode, ::mapper)
    }

    suspend fun refresh(fetchMode: FetchMode, window: Int): List<HnRankedItem> {
        return withContext(Dispatchers.IO) {
            val ids = idStore.refresh(fetchMode)
            val sliced = ids.slice(0..window)
            return@withContext itemStore.getItemRange(sliced, fetchMode, 0, refresh = true)
                .fastMap(HnItemEntity::toItem)
        }
    }

    suspend fun getItem(id: ItemId): HnRankedItem {
        return itemStore.getItem(id)
    }

    suspend fun computeWindow(
        fetchMode: FetchMode,
        start: Int,
        loadSize: Int,
    ): List<HnItemEntity>? = withContext(Dispatchers.IO) {
        val ids = idStore.getIds(fetchMode)
        val indices = ids.indices

        if (start !in indices) return@withContext null // nothing to load
        val end = (start + loadSize).coerceIn(range = indices)
        val sliced = ids.slice(start..end)
        return@withContext itemStore.getItemRange(sliced, fetchMode, start)
    }

    fun stream(fetchMode: FetchMode): Flow<List<HnItemEntity>> {
        return flow {
            val ids = idStore.getIds(fetchMode)
            emitAll(itemStore.stream(fetchMode, ids))
        }
    }

    private fun mapper(
        id: ItemId,
        fetchMode_: FetchMode,
        rank: Int,
        type: String,
        author: String?,
        time: Instant,
        dead: Boolean?,
        deleted: Boolean?,
        kids: ItemIdArray?,
        title: String?,
        url: String?,
        text: String?,
        score: Int?,
        descendants: Int?,
        parent: ItemId?,
        poll: ItemId?,
        parts: ItemIdArray?,
    ): HnRankedItem {
        return HnItemEntity(
            id, fetchMode_, rank, type, author, time,
            dead, deleted, kids, title, url, text,
            score, descendants, parent, poll, parts
        ).toItem()
    }
}