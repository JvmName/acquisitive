package dev.jvmname.acquisitive.repo

import androidx.compose.ui.util.fastMapIndexed
import app.cash.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.paging3.QueryPagingSource
import dev.jvmname.acquisitive.db.HnItemEntity
import dev.jvmname.acquisitive.db.HnItemQueries
import dev.jvmname.acquisitive.di.AppCoroutineScope
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.jvmname.acquisitive.util.fetchAsync
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Instant

@Inject
class HnItemStore(
    private val client: HnClient,
    private val db: HnItemQueries,
    @AppCoroutineScope private val scope: CoroutineScope,
) {

    fun pagingSource(
        mode: FetchMode,
        mapper: (ItemId, FetchMode, Int, String, String?, Instant, Boolean?, Boolean?, ItemIdArray?, String?, String?, String?, Int?, Int?, ItemId?, ItemId?, ItemIdArray?) -> HnRankedItem,
    ): PagingSource<ItemId, HnRankedItem> {
        return QueryPagingSource(
            transacter = db,
            context = Dispatchers.IO,
            pageBoundariesProvider = { anchor, limit ->
                db.keyedPageBoundaries(fetchMode = mode, limit = limit, anchor = anchor)
            },
            queryProvider = { beginIncl, endIncl ->
                db.keyedPaging(mode, beginIncl, endIncl, mapper = mapper)
            }
        )
    }

    suspend fun getItem(id: ItemId): HnRankedItem {
        return db.getById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .firstOrNull()
            ?.toItem()
            ?: error("$id was null")
    }

    suspend fun getItemRange(
        ids: ItemIdArray,
        mode: FetchMode,
        startIndex: Int,
        refresh: Boolean = false,
    ): List<HnItemEntity> {
        if (ids.isEmpty()) return emptyList()

        val storedList = if (refresh) {
            db.deleteByFetchMode(fetchMode = mode)
            emptyList()
        } else {
            db.getByIdAndFetchMode(ids, mode)
                .asFlow()
                .mapToList(Dispatchers.IO)
                .firstOrNull()
                ?: emptyList()
        }

        if (refresh || storedList.size != ids.size) {
            val updated = ids.fetchAsync { client.getItem(it) }
                .fastMapIndexed { i, item -> item.toEntity(startIndex + i, mode) }
            upsertItems(updated)
            return updated
        }
        return storedList
    }

    fun stream(fetchMode: FetchMode, ids: ItemIdArray): Flow<List<HnItemEntity>> {
        return db.getByIdAndFetchMode(ids, fetchMode)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    private fun upsertItems(items: List<HnItemEntity>) {
        for (chunk in items.chunked(500)) {
            db.transaction {
                for (item in chunk) {
                    db.upsertItems(item)
                }
            }
        }
    }
}