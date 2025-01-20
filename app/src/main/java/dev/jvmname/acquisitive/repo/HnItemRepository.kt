package dev.jvmname.acquisitive.repo

import androidx.paging.PagingState
import app.cash.paging.PagingSource
import dev.jvmname.acquisitive.network.model.FetchMode
import me.tatarka.inject.annotations.Inject

@Inject
class HnItemRepository(
    private val idStore: ItemIdStore,
    private val itemStore: HnItemStore,
) {

    fun pagingSource(mode: FetchMode): suspend () -> PagingSource<Int, HnItemAndRank> {
        return suspend {
            val inner = itemStore.pagingSource(idStore.getIds(mode))
            MappingPagingSource(inner, HnItemEntity::toItem)
        }
    }

    suspend fun refresh(fetchMode: FetchMode, window: Int): List<HnItemAndRank> {
        val ids = idStore.refresh(fetchMode)
        val sliced = ids.slice(0..window)
        return itemStore.getItemRange(sliced, refresh = true).map(HnItemEntity::toItem)
    }

    suspend fun computeWindow(
        fetchMode: FetchMode,
        start: Int,
        loadSize: Int,
    ): List<HnItemEntity>? {
        val ids = idStore.getIds(fetchMode)
        val indices = ids.indices

        if (start !in indices) return null // nothing to load
        val end = (start + loadSize).coerceIn(indices)
        val sliced = ids.slice(start..end)
        return itemStore.getItemRange(sliced)
    }
}

class MappingPagingSource(
    private val delegate: PagingSource<Int, HnItemEntity>,
    private val mapper: (HnItemEntity) -> HnItemAndRank,
) : PagingSource<Int, HnItemAndRank>() {

    override val jumpingSupported = true

    override fun getRefreshKey(state: PagingState<Int, HnItemAndRank>): Int? {
        return state.anchorPosition
            ?.let { maxOf(0, it - (state.config.initialLoadSize / 2)) }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HnItemAndRank> {
        val load = delegate.load(params)
        return when (load) {
            is LoadResult.Error -> LoadResult.Error(load.throwable)
            is LoadResult.Invalid -> LoadResult.Invalid()
            is LoadResult.Page -> LoadResult.Page(
                data = load.data.map(mapper),
                prevKey = load.prevKey,
                nextKey = load.nextKey,
                itemsBefore = load.itemsBefore,
                itemsAfter = load.itemsAfter
            )
        }
    }
}