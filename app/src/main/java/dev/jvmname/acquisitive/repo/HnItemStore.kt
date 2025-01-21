package dev.jvmname.acquisitive.repo

import app.cash.paging.PagingSource
import com.mercury.sqkon.db.OrderBy
import com.mercury.sqkon.db.OrderDirection
import com.mercury.sqkon.db.Sqkon
import com.mercury.sqkon.db.inList
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.jvmname.acquisitive.util.fetchAsync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.minutes

@Inject
class HnItemStore(
    skn: Sqkon,
    private val client: HnClient,
) {
    private val storage = skn.keyValueStorage<HnItemEntity>(HnItemEntity::class.simpleName!!)

    fun pagingSource(ids: ItemIdArray): PagingSource<Int, HnItemEntity> {
        return storage.selectPagingSource(
            HnItemEntity::idStr.inList(ids.map(ItemId::toString)),
            orderBy = listOf(OrderBy(HnItemEntity::responseIndex, OrderDirection.ASC))
//            expiresAfter = Clock.System.now() + CACHE_EXPIRATION
        )
    }

    suspend fun getItemRange(
        ids: ItemIdArray,
        refresh: Boolean = false,
    ): List<HnItemEntity> {
        if (ids.isEmpty()) return emptyList()

        val now = Clock.System.now()
        val keys = ids.map { it.id.toString() }

        val storedList = if (refresh) {

            emptyList()
        } else {
            storage.selectByKeys(keys, expiresAfter = now).firstOrNull() ?: emptyList()
        }

        if (refresh || storedList.size != ids.size) {
            val updated = ids.fetchAsync { client.getItem(it) }
                .mapIndexed { i, item -> item.toEntity(i) }
            storage.upsertAll(
                values = updated.associateBy(HnItemEntity::idStr),
                expiresAt = now + CACHE_EXPIRATION
            )
            return updated
        }
        return storedList
    }
}

val CACHE_EXPIRATION = 30.minutes