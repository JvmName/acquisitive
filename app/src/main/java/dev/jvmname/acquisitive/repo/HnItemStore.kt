package dev.jvmname.acquisitive.repo

import androidx.compose.ui.util.fastMapIndexed
import app.cash.paging.PagingSource
import com.mercury.sqkon.db.OrderBy
import com.mercury.sqkon.db.OrderDirection
import com.mercury.sqkon.db.Sqkon
import com.mercury.sqkon.db.Where
import com.mercury.sqkon.db.and
import com.mercury.sqkon.db.eq
import com.mercury.sqkon.db.inList
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.jvmname.acquisitive.util.fetchAsync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.minutes

@Inject
class HnItemStore(
    skn: Sqkon,
    private val client: HnClient,
) {
    private val storage = skn.keyValueStorage<HnItemEntity>(HnItemEntity::class.simpleName!!)

    fun pagingSource(ids: ItemIdArray, mode: FetchMode): PagingSource<Int, HnItemEntity> {
        return storage.selectPagingSource(
            buildWhere(mode, ids),
            orderBy = listOf(OrderBy(HnItemEntity::responseIndex, OrderDirection.ASC)),
        )
    }

    suspend fun getItemRange(
        ids: ItemIdArray,
        mode: FetchMode,
        startIndex: Int,
        refresh: Boolean = false,
    ): List<HnItemEntity> {
        if (ids.isEmpty()) return emptyList()

        val keys = ids.map { it.id.toString() }

        val storedList = if (refresh) {
            storage.delete(HnItemEntity::fetchMode eq mode.value)
            emptyList()
        } else {
            storage.selectByKeys(
                keys = keys,
                orderBy = listOf(OrderBy(HnItemEntity::responseIndex)),
//                expiresAfter = futureExpiry
            ).firstOrNull() ?: emptyList()
        }

        if (refresh || storedList.size != ids.size) {
            val updated = ids.fetchAsync { client.getItem(it) }
                .fastMapIndexed { i, item -> item.toEntity(startIndex + i, mode) }
            storage.upsertAll(
                values = updated.associateBy { it.id.toString() },
                expiresAt = futureExpiry
            )
            return updated
        }
        return storedList
    }

    fun stream(fetchMode: FetchMode, ids: ItemIdArray): Flow<List<HnItemEntity>> {
        return storage.select(buildWhere(fetchMode, ids),
            orderBy = listOf(OrderBy(HnItemEntity::responseIndex, OrderDirection.ASC)),
        )
    }

    suspend fun clearExpired() {
        storage.deleteExpired(Clock.System.now())
    }

    private fun buildWhere(mode: FetchMode, ids: ItemIdArray): Where<HnItemEntity> {
        return HnItemEntity::fetchMode eq mode.value and
                (HnItemEntity::id inList ids.storage.asList())
    }
}

val CACHE_EXPIRATION = 30.minutes
val futureExpiry: Instant
    get() = Clock.System.now() + CACHE_EXPIRATION
