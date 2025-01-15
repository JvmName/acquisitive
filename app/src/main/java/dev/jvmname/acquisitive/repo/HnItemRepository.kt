package dev.jvmname.acquisitive.repo

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.network.model.HnItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject


@Inject
class HnItemRepository(
    private val idStore: ItemIdStore,
    private val itemStore: HnItemStore,
) {

    fun pagingSource(mode: FetchMode): () -> PagingSource<Int, HnItem> {
        return {
            CompositePagingSource() {
                itemStore.pagingSource(idStore.getIds(mode))
            }
        }
    }

    suspend fun refresh(fetchMode: FetchMode): List<HnItem> {
        val ids = idStore.refresh(fetchMode)
        val sliced = ids.slice(0..DEFAULT_WINDOW)
        return itemStore.getItemRange(sliced, refresh = true)
    }

    suspend fun computeWindow(fetchMode: FetchMode, lastItemIndex: Int): List<HnItem>? {
        val ids = idStore.getIds(fetchMode)
        if (lastItemIndex == -1 || lastItemIndex !in ids.indices) return null
        val sliced = ids.slice(lastItemIndex..lastItemIndex + DEFAULT_WINDOW)
        return itemStore.getItemRange(sliced)
    }

    companion object {
        internal const val DEFAULT_WINDOW = 24
    }
}

class CompositePagingSource(
    private val pagingSourceFactory: suspend () -> PagingSource<Int, HnItem>,
) : PagingSource<Int, HnItem>() {
    private lateinit var delegate: PagingSource<Int, HnItem>

    override val jumpingSupported = true

    override fun getRefreshKey(state: PagingState<Int, HnItem>): Int? {
        return state.anchorPosition
            ?.let { maxOf(0, it - (state.config.initialLoadSize / 2)) }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HnItem> =
        withContext(Dispatchers.IO) {
            if (!::delegate.isInitialized) {
                delegate = pagingSourceFactory()
            }
            delegate.load(params)
        }


}