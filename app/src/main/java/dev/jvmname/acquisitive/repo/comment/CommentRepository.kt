package dev.jvmname.acquisitive.repo.comment

import dev.jvmname.acquisitive.db.CommentEntity
import dev.jvmname.acquisitive.db.ObserveComments
import dev.jvmname.acquisitive.network.HnClient
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.descendants
import dev.jvmname.acquisitive.util.fetchAsync
import dev.jvmname.acquisitive.util.mapList
import dev.jvmname.acquisitive.util.toCommentEntity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import logcat.logcat

@Inject
@SingleIn(AppScope::class)
class CommentRepository(
    private val store: CommentStore,
    private val client: HnClient,
) {
    suspend fun refresh(parentStoryId: ItemId) = withContext(Dispatchers.IO) {
        //seed the queue with the top-level comments
        val (seedParent, seedKids) = client.getChildren(parentStoryId)
        val idQueue = ArrayDeque<CommentEntity>(minOf(40, seedKids.size * 3))
        seedKids.toCommentEntity().also { kids ->
            idQueue += kids
            store.refresh(parent = seedParent to null, children = kids)
        }

        var depth = 0
        val maxDepth = when {
            (seedParent.descendants ?: 0) > 150 -> 2
            else -> 4

        }
        logcat { "refresh - top level comments: ${idQueue.size}" }
        //TODO imagine how this could be parallelized better
        while (idQueue.isNotEmpty() && depth < maxDepth) { //TODO make depth configurable
            val itemsAtDepth = idQueue.size
            logcat { "refresh - beginning depth:$depth; queue size: ${idQueue.size}" }
            //for each parent...
            repeat(itemsAtDepth) {
                val current = idQueue.removeFirst()
                //...fetch the children
                val inflatedChildren = client.getChildren(current.id).second.toCommentEntity()
                //...save parent + children to DB
                store.refresh(
                    parent = null to current,
                    children = inflatedChildren,
                )
                //...and add the children to the queue
                idQueue += inflatedChildren
                logcat { "refresh - inner loop at depth:$depth; queue size: ${idQueue.size}" }
            }
            logcat { "refresh - finished depth:$depth; queue size: ${idQueue.size}" }
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