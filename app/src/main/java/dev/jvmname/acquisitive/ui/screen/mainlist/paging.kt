package dev.jvmname.acquisitive.ui.screen.mainlist

import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import app.cash.paging.RemoteMediator
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.repo.HnItemAndRank
import dev.jvmname.acquisitive.repo.HnItemRepository
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
        return Pager(config = PagingConfig(pageSize = 24),
            remoteMediator = mediatorFactory(mode),
            pagingSourceFactory = {
                runBlocking { repo.pagingSource(mode).invoke() }
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