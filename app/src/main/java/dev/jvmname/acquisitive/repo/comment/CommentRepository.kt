package dev.jvmname.acquisitive.repo.comment

import dev.jvmname.acquisitive.db.CommentEntity
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.util.fetchAsync
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
class CommentRepository(
    private val store: CommentStore,
    private val client: HnClient,
) {

    suspend fun refresh(parentStoryId: ItemId) {
        return withContext(Dispatchers.IO) {
            val (parent, comments) = client.getChildren(parentStoryId)
            store.refresh(parent.id, comments.filterIsInstance<HnItem.Comment>())
        }
    }

    fun observeComments(parentStoryId: ItemId): Flow<List<CommentEntity>> {
        return store.observeComments(parentStoryId)
    }

    suspend fun toggleCommentExpanded(commentId: ItemId) = withContext(Dispatchers.IO) {
        val (expanded, kids, childrenCountInDb) = store.updateExpanded(commentId)
        if (expanded && !kids.isNullOrEmpty() && childrenCountInDb < kids.size) {
            val children = kids.fetchAsync { client.getItem(it) as HnItem.Comment }
            store.insertExpanded(commentId, true, children)
        }
    }
}