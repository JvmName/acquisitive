package dev.jvmname.acquisitive.ui.screen.mainlist

import androidx.compose.ui.util.fastLastOrNull
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import app.cash.paging.RemoteMediator
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.repo.HnItemRepository
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject


@Inject
class HnItemPagerFactory(
    private val mediatorFactory: (FetchMode) -> HnItemMediator,
    private val repo: HnItemRepository,
) {
    operator fun invoke(mode: FetchMode): Pager<Int, HnItem> {
        return Pager(
            config = PagingConfig(pageSize = HnItemRepository.DEFAULT_WINDOW),
            remoteMediator = mediatorFactory(mode),
            pagingSourceFactory = repo.pagingSource(mode)
        )
    }
}

@Inject
class HnItemMediator(
    @Assisted private val mode: FetchMode,
    private val repo: HnItemRepository,
) : RemoteMediator<Int, HnItem>() {

    override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, HnItem>,
    ): MediatorResult {
        val response = when (loadType) {
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                //equivalent to `state.lastItemOrNull()`
                val lastIndex = state.pages
                    .fastLastOrNull { it.data.isNotEmpty() }
                    ?.data
                    ?.lastIndex
                    ?: return MediatorResult.Success(true)
                repo.computeWindow(mode, lastIndex)
                    .orEmpty()
            }

            LoadType.REFRESH -> repo.refresh(mode)
        }

        return MediatorResult.Success(response.isEmpty())
    }
}