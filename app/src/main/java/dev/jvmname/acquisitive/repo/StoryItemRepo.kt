package dev.jvmname.acquisitive.repo

import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ShadedHnItem
import dev.jvmname.acquisitive.network.model.id
import dev.jvmname.acquisitive.util.ItemIdArray
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject


@Inject
class StoryItemRepo(private val store: HnItemStore) {

    suspend fun getStories(mode: FetchMode, storyIds: List<ShadedHnItem>): List<ShadedHnItem> {
        val ids = ItemIdArray(storyIds.size) { storyIds[it].id }
        return store.getItemRange(mode, ids)
    }

    fun observeStories(mode: FetchMode, window: Int): Flow<List<ShadedHnItem>> {
        return store.streamItems(mode, window)
    }
}