package dev.jvmname.acquisitive.repo

import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import app.cash.paging.RemoteMediator
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import logcat.asLog
import logcat.logcat
import retrofit2.HttpException
import java.io.IOException


@Inject
class HnItemPagerFactory(
    private val mediatorFactory: HnItemMediator.Factory,
    private val repo: HnItemRepository,
) {
    operator fun invoke(mode: FetchMode): Pager<ItemId, HnRankedItem> {
        val psf = repo.pagingSource(mode)
        return Pager(
            config = PagingConfig(
                pageSize = 24,
                initialLoadSize = 24,
                enablePlaceholders = true
            ),
            remoteMediator = mediatorFactory(mode, psf::invalidate),
            pagingSourceFactory = psf
        )
    }
}

@Inject
class HnItemMediator(
    @Assisted private val mode: FetchMode,
    @Assisted private val onInvalidate: () -> Unit,
    private val repo: HnItemRepository,
) : RemoteMediator<ItemId, HnRankedItem>() {

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(mode: FetchMode, onInvalidate: () -> Unit): HnItemMediator
    }

    //https://issuetracker.google.com/issues/391414839
    override suspend fun initialize(): InitializeAction {
//        repo.clearExpired()
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<ItemId, HnRankedItem>,
    ): MediatorResult {
        logcat { "load: $loadType" }
        val pageSize = state.config.pageSize
        return try {
            val endOfPage = when (loadType) {
                LoadType.PREPEND -> return eop()
                LoadType.APPEND -> {
                    //anything more to load?
                    val startId = state.lastItemOrNull()?.item?.id
                    repo.appendWindow(
                        fetchMode = mode,
                        startId = startId,
                        loadSize = pageSize
                    ).also {
                        if (it) onInvalidate()
                    }
                }

                LoadType.REFRESH -> repo.refresh(fetchMode = mode, pageSize)
                    .also { onInvalidate() }
            }

            return MediatorResult.Success(endOfPaginationReached = endOfPage).also {
                logcat { "MediatorResult: $it" }
            }
        } catch (e: IOException) {
            logcat { e.asLog() }
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            logcat { e.asLog() }
            MediatorResult.Error(e)
        }
    }

    private fun eop() = MediatorResult.Success(endOfPaginationReached = true)
}