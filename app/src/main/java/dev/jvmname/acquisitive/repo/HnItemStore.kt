package dev.jvmname.acquisitive.repo

import app.cash.paging.PagingSource
import com.mercury.sqkon.db.KeyValueStorage
import com.mercury.sqkon.db.Sqkon
import com.mercury.sqkon.db.inList
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.jvmname.acquisitive.util.fetchAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.minutes

@Inject
class HnItemStore(
    skn: Sqkon,
    private val client: HnClient,
) {
    private val storage = skn.keyValueStorage<HnItem>(
        HnItem::class.simpleName!!,
        config = KeyValueStorage.Config(dispatcher = Dispatchers.IO)
    )

    fun pagingSource(ids: ItemIdArray): PagingSource<Int, HnItem> {
        return storage.selectPagingSource(
            HnItem::id.inList(ids.map { it.id.toString() }),
            expiresAfter = Clock.System.now() + CACHE_EXPIRATION
        )
    }

    suspend fun getItemRange(
        ids: ItemIdArray,
        refresh: Boolean = false,
    ): List<HnItem> = storage.transactionWithResult twr@{
        if (ids.isEmpty()) return@twr emptyList()

        val now = Clock.System.now()
        val keys = ids.map { it.id.toString() }

        val storedList = if (refresh) {
            coroutineScope {
                launch(Dispatchers.IO) { storage.deleteExpired() }
            }
            emptyList()
        } else {
            storage.selectByKeys(
                keys,
                expiresAfter = now
            ).firstOrNull() ?: emptyList()
        }

        if (refresh || storedList.size != ids.size) {
            val updated = ids.fetchAsync { client.getItem(it) }
            storage.upsertAll(
                values = updated.associateBy { it.id.toString() },
                expiresAt = now + CACHE_EXPIRATION
            )
            return@twr updated
        }
        return@twr storedList
    }
}

val CACHE_EXPIRATION = 30.minutes