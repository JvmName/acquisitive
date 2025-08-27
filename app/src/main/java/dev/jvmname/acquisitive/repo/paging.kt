package dev.jvmname.acquisitive.repo

import androidx.compose.ui.util.fastMap
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import app.cash.paging.PagingSource
import app.cash.paging.RemoteMediator
import dev.jvmname.acquisitive.db.HnItemEntity
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.ItemId
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import retrofit2.HttpException
import java.io.IOException


@Inject
class HnItemPagerFactory(
    private val mediatorFactory: HnItemMediator.Factory,
    private val repo: HnItemRepository,
) {
    operator fun invoke(mode: FetchMode): Pager<ItemId, HnRankedItem> {
        return Pager(
            config = PagingConfig(pageSize = 24, enablePlaceholders = true),
            remoteMediator = mediatorFactory(mode),
            pagingSourceFactory = InvalidatingPagingSourceFactory { repo.pagingSource(mode) }
        )
    }
}

@Inject
class HnItemMediator(
    @Assisted private val mode: FetchMode,
    private val repo: HnItemRepository,
) : RemoteMediator<ItemId, HnRankedItem>() {

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(mode: FetchMode): HnItemMediator
    }

    //https://issuetracker.google.com/issues/391414839
    override suspend fun initialize(): InitializeAction {
//        repo.clearExpired()
        return InitializeAction.SKIP_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<ItemId, HnRankedItem>,
    ): MediatorResult {
        return try {
            val endOfPage = when (loadType) {
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val rank = state.lastItemOrNull()?.rank ?: 0
                    val startIndex = (rank - 1).coerceAtLeast(0)
                    //anything more to load?
                    val list = repo.computeWindow(
                        fetchMode = mode,
                        start = startIndex,
                        loadSize = state.config.pageSize
                    ) ?: return MediatorResult.Success(endOfPaginationReached = true)
                    list.isEmpty() || startIndex == list.last().rank
                }

                LoadType.REFRESH -> repo.refresh(
                    fetchMode = mode,
                    window = state.config.initialLoadSize
                ).isEmpty()
            }

            return MediatorResult.Success(endOfPaginationReached = endOfPage)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}

class MappingPagingSource(
    private val delegate: PagingSource<Int, HnItemEntity>,
    private val mapper: (HnItemEntity) -> HnRankedItem,
) : PagingSource<Int, HnRankedItem>() {

    init {
        //insane bidirectional mapping of invalidations because you can't "map" a PagingSource
        delegate.registerInvalidatedCallback { invalidate() }
        registerInvalidatedCallback { delegate.invalidate() }
    }

    override val jumpingSupported = delegate.jumpingSupported

    override fun getRefreshKey(state: PagingState<Int, HnRankedItem>): Int? {
        return state.anchorPosition
            ?.let { maxOf(0, it - (state.config.initialLoadSize / 2)) }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HnRankedItem> {
        return when (val load = delegate.load(params)) {
            is LoadResult.Error -> LoadResult.Error(load.throwable)
            is LoadResult.Invalid -> LoadResult.Invalid()
            is LoadResult.Page -> LoadResult.Page(
                data = load.data.fastMap(mapper).sortedBy { it.rank },
                prevKey = load.prevKey,
                nextKey = load.nextKey,
                itemsBefore = load.itemsBefore,
                itemsAfter = load.itemsAfter
            )
        }
    }
}