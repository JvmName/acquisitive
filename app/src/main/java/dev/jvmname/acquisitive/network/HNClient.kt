package dev.jvmname.acquisitive.network

import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.User
import dev.jvmname.acquisitive.network.model.UserId
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.jvmname.acquisitive.util.emptyItemIdArray
import dev.jvmname.acquisitive.util.fetchAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject


abstract class HnClient {
    //TODO figure out if i want to take the boxing hit to have an ApiResult type
    protected suspend fun <T> wrap(call: suspend () -> T): T {
        return withContext(Dispatchers.IO) { call() }
    }
}

class RealHnClient @Inject constructor(factory: RetrofitFactory) : HnClient() {
    private val storyClient = factory.create<HnStoryApi>("https://hacker-news.firebaseio.com/v0/")
//    private val userClient = factory.create<HnUserApi>("https://news.ycombinator.com/")

    suspend fun getStories(mode: FetchMode): ItemIdArray {
        return when (mode) {
            FetchMode.TOP -> getTopStories()
            FetchMode.NEW -> getNewStories()
            FetchMode.ASK -> getAskStories()
            FetchMode.SHOW -> getShowStories()
            FetchMode.JOBS -> getJobStories()
            FetchMode.BEST -> getBestStories()
        }
    }

    suspend fun getChildren(item: HnItem): Pair<HnItem, List<HnItem>> {
        return item to item.kids.orEmpty()
            .fetchAsync { getItem(it) }
    }

    suspend fun getTopStories(): ItemIdArray {
        return wrap { storyClient.getTopStories() }
            ?: emptyItemIdArray()
    }

    suspend fun getNewStories(): ItemIdArray {
        return wrap { storyClient.getNewStories() }
            ?: emptyItemIdArray()
    }

    suspend fun getShowStories(): ItemIdArray {
        return wrap { storyClient.getShowStories() }
            ?: emptyItemIdArray()
    }

    suspend fun getAskStories(): ItemIdArray {
        return wrap { storyClient.getAskStories() }
            ?: emptyItemIdArray()
    }

    suspend fun getJobStories(): ItemIdArray {
        return wrap { storyClient.getJobStories() }
            ?: emptyItemIdArray()
    }

    suspend fun getBestStories(): ItemIdArray {
        return wrap { storyClient.getBestStories() }
            ?: emptyItemIdArray()
    }

    suspend fun getItem(id: ItemId): HnItem {
        return wrap { storyClient.getItem(id) }
    }

    suspend fun getUser(id: UserId): User? {
        return wrap { storyClient.getUser(id) }
    }


}