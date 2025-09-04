package dev.jvmname.acquisitive.repo

import androidx.compose.ui.util.fastForEach
import androidx.paging.InvalidatingPagingSourceFactory
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.paging3.QueryPagingSource
import dev.jvmname.acquisitive.db.GetIdRange
import dev.jvmname.acquisitive.db.HnIdEntity
import dev.jvmname.acquisitive.db.HnItemEntity
import dev.jvmname.acquisitive.db.HnItemQueries
import dev.jvmname.acquisitive.db.IdItemQueries
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import logcat.logcat
import kotlin.time.Instant


@Inject
class HnItemStore(
    private val db: IdItemQueries,
    private val itemDb: HnItemQueries,
) {
    fun pagingSource(
        mode: FetchMode,
        mapper: HnItemEntityMapper,
    ): InvalidatingPagingSourceFactory<ItemId, HnRankedItem> {
        return InvalidatingPagingSourceFactory {
            QueryPagingSource(
                transacter = itemDb,
                context = Dispatchers.IO,
                queryProvider = { beginIncl, endIncl ->
                    itemDb.keyedPaging(mode, beginIncl, endIncl, mapper = mapper)
                        .also { it.addListener { logcat { "!!Query results changed - keyedPaging" } } }
                },
                pageBoundariesProvider = { anchor, limit ->
                    itemDb.keyedPageBoundaries(fetchMode = mode, limit = limit, anchor = anchor)
                        .also { it.addListener { logcat { "!!Query results changed - keyedPageBoundaries" } } }
                }
            )
        }
    }

    suspend fun getIdRange(mode: FetchMode, start: ItemId?, window: Int): List<GetIdRange> {
        if (start == null) return emptyList()
        return db.getIdRange(fetchMode = mode, startId = start, window = window).awaitAsList()
    }

    fun refresh(mode: FetchMode, ids: ItemIdArray, items: List<HnItem>) = db.transaction {
        db.deleteIdByFetchMode(mode)

        val lastItemIdx = items.lastIndex
        ids.forEachIndexed { i, id ->
            db.insertId(HnIdEntity(id, mode, i))
            //use the same idx for [items], acknowledging that it will have fewer elements than [ids]
            if (i <= lastItemIdx) {
                db.insertItem(items[i].toEntity(i, mode))
            }
        }
    }

    /** @return the distance between the last of [items] and the last id/item in the db */
    fun updateRange(mode: FetchMode, items: List<HnItemEntity>): Int {
        if (items.isEmpty()) return 0

        val ids = ItemIdArray(items.size) { items[it].id }
        return db.transactionWithResult {
            db.deleteAllByIds(mode, ids)
            items.fastForEach {
                db.insertIdItem(
                    id = it.id,
                    fetchMode = it.fetchMode,
                    rank = it.rank,
                    type = it.type,
                    author = it.author,
                    time = it.time,
                    dead = it.dead,
                    deleted = it.deleted,
                    kids = it.kids,
                    title = it.title,
                    url = it.url,
                    text = it.text,
                    score = it.score,
                    descendants = it.descendants,
                    parent = it.parent,
                    poll = it.poll,
                    parts = it.parts
                )
            }
            db.getRankDistanceForId(items.last().id) { rank, idMaxRank, itemMaxRank ->
                when (val rankL = rank.toLong()) {
                    itemMaxRank -> idMaxRank - itemMaxRank
                    else -> itemMaxRank - rankL
                }
            }
                .executeAsOne()
                .toInt()
        }
    }

    suspend fun getItem(mode: FetchMode, id: ItemId): HnItemEntity {
        return db.getItemForId(id, mode).awaitAsOne()
    }
}

private fun toRankedItem(
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
    id_: ItemId,
): HnRankedItem = TODO()

internal typealias HnItemEntityMapper = (
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
) -> HnRankedItem