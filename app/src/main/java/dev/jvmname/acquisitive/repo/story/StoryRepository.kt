package dev.jvmname.acquisitive.repo.story

import androidx.compose.ui.util.fastZip
import androidx.paging.InvalidatingPagingSourceFactory
import dev.jvmname.acquisitive.db.StoryEntity
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
class StoryRepository(
    private val client: HnClient,
    private val store: StoryStore,
) {

    fun pagingSource(mode: FetchMode): InvalidatingPagingSourceFactory<Int, RankedStory> {
        return store.pagingSource(mode, ::EntityToRankedStory)
    }

    suspend fun refresh(fetchMode: FetchMode, pageSize: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val ids = client.getStories(fetchMode)
//            val items = ids.take(pageSize).fetchAsync { client.getItem(it) }
            store.refresh(fetchMode, ids, emptyList())
            false
        }
    }

    /** @return true if there are more items to fetch, false otherwise */
    suspend fun appendWindow(
        fetchMode: FetchMode,
        startId: ItemId?,
        loadSize: Int,
    ): Boolean = withContext(Dispatchers.IO) {
        val ids = store.getIdRange(fetchMode, startId, loadSize)
        val items = ids.fetchAsync { client.getItem(it.id) }
        val zipped = ids.fastZip(items) { id, item -> item.toEntity(id.rank, fetchMode) }
        store.updateRange(fetchMode, zipped) > 0
    }

    suspend fun getItem(mode: FetchMode, id: ItemId): HnItem {
        return store.getItem(mode, id).toStory().item
    }
}

private fun EntityToRankedStory(
    id: ItemId,
    fetchMode: FetchMode,
    rank: Int,
    type: ItemType,
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
) = StoryEntity(
    id = id, fetchMode = fetchMode, rank = rank, type = type, author = author, time = time,
    dead = dead, deleted = deleted, kids = kids, title = title, url = url, text = text,
    score = score, descendants = descendants, parent = parent, poll = poll, parts = parts
).toStory()