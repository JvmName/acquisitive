package dev.jvmname.acquisitive.repo

import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import app.cash.paging.PagingSource
import app.cash.paging.RemoteMediator
import dev.jvmname.acquisitive.network.model.FetchMode
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import retrofit2.HttpException
import java.io.IOException


@Inject
class HnItemPagerFactory(
    private val mediatorFactory: (FetchMode) -> HnItemMediator,
    private val repo: HnItemRepository,
) {
    operator fun invoke(mode: FetchMode): Pager<Int, HnItemAndRank> {
        return Pager(config = PagingConfig(pageSize = 24, enablePlaceholders = false),
            remoteMediator = mediatorFactory(mode),
            pagingSourceFactory = {
                runBlocking {
                    val inner = repo.pagingSource(mode).invoke()
                    MappingPagingSource(inner, HnItemEntity::toItem)
                }
            })
    }
}

@Inject
class HnItemMediator(
    @Assisted private val mode: FetchMode,
    private val repo: HnItemRepository,
) : RemoteMediator<Int, HnItemAndRank>() {

    override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, HnItemAndRank>,
    ): MediatorResult {
        return try {
            val endOfPage = when (loadType) {
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val offset = state.lastItemOrNull()
                    val windowIndex = ((offset?.rank ?: 0) - 1).coerceAtLeast(0)

                    //anything more to load?
                    val list = repo.computeWindow(
                        fetchMode = mode,
                        start = windowIndex,
                        loadSize = state.config.pageSize
                    ) ?: return MediatorResult.Success(endOfPaginationReached = true)
                    list.isEmpty() || windowIndex == list.lastIndex
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
    private val mapper: (HnItemEntity) -> HnItemAndRank,
) : PagingSource<Int, HnItemAndRank>() {

    init {
        //insane bidirectional mapping of invalidations because you can't "map" a PagingSOurce
        delegate.registerInvalidatedCallback { invalidate() }
        registerInvalidatedCallback { delegate.invalidate() }
    }

    override val jumpingSupported = true

    override fun getRefreshKey(state: PagingState<Int, HnItemAndRank>): Int? {
        return state.anchorPosition
            ?.let { maxOf(0, it - (state.config.initialLoadSize / 2)) }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HnItemAndRank> {
        return when (val load = delegate.load(params)) {
            is LoadResult.Error -> LoadResult.Error(load.throwable)
            is LoadResult.Invalid -> LoadResult.Invalid()
            is LoadResult.Page -> LoadResult.Page(
                data = load.data.map(mapper).sortedBy { it.rank },
                prevKey = load.prevKey,
                nextKey = load.nextKey,
                itemsBefore = load.itemsBefore,
                itemsAfter = load.itemsAfter
            )
        }
    }
}