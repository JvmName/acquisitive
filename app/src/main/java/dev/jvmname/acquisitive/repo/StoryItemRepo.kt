package dev.jvmname.acquisitive.repo

import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.ShadedHnItem
import dev.jvmname.acquisitive.ui.screen.mainlist.debugToString1
import dev.jvmname.acquisitive.ui.screen.mainlist.debugToString2
import dev.jvmname.acquisitive.util.fetchAsync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import logcat.logcat
import me.tatarka.inject.annotations.Inject


@Inject
class StoryItemRepo(private val store: HnItemStore) {
    suspend fun getStory(id: ItemId): ShadedHnItem {
        val result = store.get(StoryItemKey.Single(id))
        result as StoryItemResult.Single
        return result.item
    }

    suspend fun getStories(storyIds: List<ShadedHnItem>): List<ShadedHnItem> {
        return storyIds.fetchAsync { item ->
            when (item) {
                is ShadedHnItem.Shallow -> getStory(item.item)
                is ShadedHnItem.Full -> item
            }
        }
    }

    fun observeStories(fetchMode: FetchMode, window: Int = 5): Flow<List<ShadedHnItem>> {
        val key = StoryItemKey.All(fetchMode, window)
        return store.stream(key)
            .map {
                (it as StoryItemResult.All).items.also {
                    logcat {
                        "***observeStories produces: " + it.debugToString1()
                    }
                }
            }
    }
}

