package dev.jvmname.acquisitive.repo

import androidx.compose.ui.util.fastZip
import androidx.paging.InvalidatingPagingSourceFactory
import dev.jvmname.acquisitive.db.HnItemEntity
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.jvmname.acquisitive.util.fetchAsync
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Instant

@Inject
class HnItemRepository(
    private val client: HnClient,
    private val store: HnItemStore,
) {

    fun pagingSource(mode: FetchMode): InvalidatingPagingSourceFactory<ItemId, HnRankedItem> {
        return store.pagingSource(mode, ::ExpandedEntityToRankedItem)
    }

    suspend fun refresh(fetchMode: FetchMode, pageSize: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val ids = client.getStories(fetchMode)
            val items = ids.take(pageSize).fetchAsync { client.getItem(it) }
            store.refresh(fetchMode, ids, items)
            false
        }
    }

    /** @return true if there are more items to fetch, false otherwise */
    suspend fun appendWindow(
        fetchMode: FetchMode,
        startId: ItemId?,
        loadSize: Int,
    ): Boolean = withContext(Dispatchers.IO) {
        if (startId == null) return@withContext true
        val ids = store.getIdRange(fetchMode, startId, loadSize)
        val items = ids.fetchAsync { client.getItem(it.id) }
        val zipped = ids.fastZip(items) { id, item -> item.toEntity(id.rank, fetchMode) }
        store.updateRange(fetchMode, zipped) > 0
    }

    suspend fun getItem(mode: FetchMode, id: ItemId): HnItem {
        return store.getItem(mode, id).toItem().item
    }
}

private fun ExpandedEntityToRankedItem(
    id: ItemId,
    fetchMode: FetchMode,
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
        id, fetchMode, rank, type, author, time,
        dead, deleted, kids, title, url, text,
        score, descendants, parent, poll, parts
    ).toItem()
}