package dev.jvmname.acquisitive.network

import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.User
import dev.jvmname.acquisitive.network.model.UserId
import dev.jvmname.acquisitive.util.ItemIdArray
import dev.jvmname.acquisitive.util.emptyItemIdArray
import dev.jvmname.acquisitive.util.fetchAsync
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import logcat.asLog
import logcat.logcat
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

abstract class HnClient {
    //TODO figure out if i want to take the boxing hit to have an ApiResult type
    protected suspend inline fun <reified T> wrap(crossinline call: suspend () -> T): T {
        return withContext(Dispatchers.IO + CoroutineName("HnClientWrapped")) {
            try {
                if (!isActive) logcat { "***scope not active" }
                call()
            } catch (e: Exception) {
                logcat { "***error: " + e.asLog() }
                throw e
            }
        }
    }

    abstract suspend fun getStories(mode: FetchMode): ItemIdArray
    abstract suspend fun getChildren(item: HnItem): Pair<HnItem, List<HnItem>>
    abstract suspend fun getTopStories(): ItemIdArray
    abstract suspend fun getNewStories(): ItemIdArray
    abstract suspend fun getShowStories(): ItemIdArray
    abstract suspend fun getAskStories(): ItemIdArray
    abstract suspend fun getJobStories(): ItemIdArray
    abstract suspend fun getBestStories(): ItemIdArray
    abstract suspend fun getItem(id: ItemId): HnItem
    abstract suspend fun getUser(id: UserId): User?
}

@[Inject ContributesBinding(AppScope::class)]
class RealHnClient(factory: NetworkComponent.RetrofitFactory) : HnClient() {
    private val storyClient = factory.create<HnStoryApi>("https://hacker-news.firebaseio.com/v0/")
//    private val userClient = factory.create<HnUserApi>("https://news.ycombinator.com/")

    override suspend fun getStories(mode: FetchMode): ItemIdArray {
        return when (mode) {
            FetchMode.TOP -> getTopStories()
            FetchMode.NEW -> getNewStories()
            FetchMode.ASK -> getAskStories()
            FetchMode.SHOW -> getShowStories()
            FetchMode.JOBS -> getJobStories()
            FetchMode.BEST -> getBestStories()
        }
    }

    override suspend fun getChildren(item: HnItem): Pair<HnItem, List<HnItem>> {
        return item to item.kids.orEmpty().fetchAsync { getItem(it) }
    }

    override suspend fun getTopStories(): ItemIdArray {
        return try {
            storyClient.getTopStories()
        } catch (e: Exception) {
            throw e
        }
            ?: emptyItemIdArray()
    }

    override suspend fun getNewStories(): ItemIdArray {
        return wrap { storyClient.getNewStories() }
            ?: emptyItemIdArray()
    }

    override suspend fun getShowStories(): ItemIdArray {
        return wrap { storyClient.getShowStories() }
            ?: emptyItemIdArray()
    }

    override suspend fun getAskStories(): ItemIdArray {
        return wrap { storyClient.getAskStories() }
            ?: emptyItemIdArray()
    }

    override suspend fun getJobStories(): ItemIdArray {
        return wrap { storyClient.getJobStories() }
            ?: emptyItemIdArray()
    }

    override suspend fun getBestStories(): ItemIdArray {
        return wrap { storyClient.getBestStories() }
            ?: emptyItemIdArray()
    }

    override suspend fun getItem(id: ItemId): HnItem {
        return wrap { storyClient.getItem(id) }
    }

    override suspend fun getUser(id: UserId): User? {
        return wrap { storyClient.getUser(id) }
    }


}