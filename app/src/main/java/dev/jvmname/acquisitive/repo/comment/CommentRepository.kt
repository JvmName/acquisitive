package dev.jvmname.acquisitive.repo.comment

import androidx.compose.ui.util.fastForEach
import dev.jvmname.acquisitive.db.CommentEntity
import dev.jvmname.acquisitive.db.ObserveComments
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.util.fetchAsync
import dev.jvmname.acquisitive.util.mapList
import dev.jvmname.acquisitive.util.toCommentEntity
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
class CommentRepository(
    private val store: CommentStore,
    private val client: HnClient,
) {
    suspend fun refresh(parentStoryId: ItemId) = withContext(Dispatchers.IO) {
        val (parent, children) = client.getChildren(parentStoryId)

        //seed the queue with the top-level comments
        val idQueue = ArrayDeque<CommentEntity>(children.size * 3)
        children.toCommentEntity().also {
            idQueue.addAll(it)
            store.refresh(parent = parent to null, children = it)
        }

        var depth = 0
        while (idQueue.isNotEmpty() && depth < 4) { //TODO make depth configurable
            val itemsAtDepth = idQueue.size
            repeat(itemsAtDepth) {
                val current = idQueue.removeFirst()
                val kidEntities = client.getChildren(current.id).second.toCommentEntity()
                store.refresh(
                    parent = null to current,
                    children = kidEntities,
                )

                // Collect children for the next depth level
                children.fastForEach { comment ->
                    if (comment !is HnItem.Comment || comment.kids == null) return@fastForEach
                    idQueue.addAll(kidEntities)
                }
            }
            depth++
        }
    }

    fun observeComments(parentStoryId: ItemId): Flow<List<RankedComment>> {
        return store.observeComments(parentStoryId)
            .mapList(ObserveComments::toRankedComment)
    }

    suspend fun toggleCommentExpanded(commentId: ItemId) = withContext(Dispatchers.IO) {
        val (expanded, kids, childrenCountInDb) = store.updateExpanded(commentId)
        if (expanded && !kids.isNullOrEmpty() && childrenCountInDb < kids.size) {
            val children = kids.fetchAsync { client.getItem(it) as HnItem.Comment }
            store.insertExpanded(commentId, true, children)
        }
    }

}

private fun ObserveComments.toRankedComment(): RankedComment {
    return RankedComment(
        comment = HnItem.Comment(
            id = id,
            by = author,
            time = time,
            dead = dead,
            deleted = deleted,
            kids = kids,
            text = text,
            parent = parent,
        ),
        depth = depth.toInt(),
        rank = rank,
        expanded = expanded,
    )
}