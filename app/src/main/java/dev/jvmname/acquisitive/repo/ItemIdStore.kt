package dev.jvmname.acquisitive.repo

import com.mercury.sqkon.db.Sqkon
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.jvmname.acquisitive.util.emptyItemIdArray
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject


@Inject
class ItemIdStore(
    skn: Sqkon,
    private val client: HnClient,
) {
    private val storage = skn.keyValueStorage<ItemIdArray>(name = ItemIdArray::class.simpleName!!)

    suspend fun getIds(fetchMode: FetchMode): ItemIdArray {
        val ids = storage.selectByKeys(
            listOf(fetchMode.value),
            expiresAfter = futureExpiry
        )
            .map { it.firstOrNull() }
            .first()
            ?: emptyItemIdArray()
        if (ids.isEmpty()) {
            return refresh(fetchMode)
        }
        return ids
    }

    suspend fun refresh(fetchMode: FetchMode): ItemIdArray {
        val updatedIds = client.getStories(fetchMode)
        val now = Clock.System.now()
        storage.upsert(
            fetchMode.value,
            updatedIds,
            expiresAt = futureExpiry
        )
        return updatedIds
    }

    suspend fun clearExpired() {
        storage.deleteExpired(Clock.System.now())
    }
}