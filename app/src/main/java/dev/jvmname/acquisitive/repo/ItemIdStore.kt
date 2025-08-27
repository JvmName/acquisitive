package dev.jvmname.acquisitive.repo

import dev.jvmname.acquisitive.db.HnItemQueries
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.zacsweers.metro.Inject


@Inject
class ItemIdStore(
    private val client: HnClient,
    private val db: HnItemQueries,
) {

    suspend fun getIds(fetchMode: FetchMode): ItemIdArray {
        TODO()
      /*  val ids = storage.selectByKeys(
            listOf(fetchMode.name),
            expiresAfter = futureExpiry
        )
            .map { it.firstOrNull() }
            .first()
            ?: emptyItemIdArray()
        if (ids.isEmpty()) {
            return refresh(fetchMode)
        }
        return ids*/
    }

    suspend fun refresh(fetchMode: FetchMode): ItemIdArray {
//        val updatedIds = client.getStories(fetchMode)

        TODO()
     /*   val updatedIds = client.getStories(fetchMode)
        val now = kotlin.time.Clock.System.now()
        storage.upsert(
            fetchMode.name,
            updatedIds,
            expiresAt = futureExpiry
        )
        return updatedIds*/
    }

    suspend fun clearExpired() {
        TODO()
//        storage.deleteExpired(kotlin.time.Clock.System.now())
    }
}