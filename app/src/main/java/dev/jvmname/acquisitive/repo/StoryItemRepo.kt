package dev.jvmname.acquisitive.repo

import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.ShadedHnItem
import dev.jvmname.acquisitive.network.model.shaded
import dev.jvmname.acquisitive.util.fetchAsync
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject


@Inject
class StoryItemRepo(private val store: HnItemStore) {
    suspend fun getStory(mode: FetchMode, id: ItemId): HnItem {
        return store.getItem(mode, id).item
    }

    suspend fun getStories(mode: FetchMode, storyIds: List<ShadedHnItem>): List<ShadedHnItem> {
        return storyIds.fetchAsync { item ->
            when (item) {
                is ShadedHnItem.Shallow -> getStory(mode, item.item).shaded()
                is ShadedHnItem.Full -> item
            }
        }
    }

    fun observeStories(mode: FetchMode, window: Int = 5): Flow<List<ShadedHnItem>> {
        return store.stream(mode, window)
    }
}