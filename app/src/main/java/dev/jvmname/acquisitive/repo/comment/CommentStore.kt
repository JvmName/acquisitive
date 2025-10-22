package dev.jvmname.acquisitive.repo.comment

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.jvmname.acquisitive.db.CommentEntity
import dev.jvmname.acquisitive.db.CommentQueries
import dev.jvmname.acquisitive.db.ObserveComments
import dev.jvmname.acquisitive.db.UpdateExpanded
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

@Inject
class CommentStore(private val db: CommentQueries) {

    fun refresh(
        parent: Pair<HnItem?, CommentEntity?>,
        children: List<CommentEntity>,
    ) = db.transaction {
        val (parentItem, parentComment) = parent
        require((parentItem != null) xor (parentComment != null)) { "either HnItem or CommentEntity must be non-null" }
        when {
            parentItem != null -> db.deleteCommentsForParent(parentItem.id)
            parentComment != null -> db.insertComment(parentComment)
        }
        children.fastForEach {
            db.insertComment(it)
        }
    }

    fun observeComments(parentId: ItemId): Flow<List<ObserveComments>> {
        return db.observeComments(parentId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun updateExpanded(id: ItemId, overrideExpanded: Boolean? = null): UpdateExpanded {
        return db.updateExpanded(
            id = id, expanded = overrideExpanded?.let { if (it) 1 else 0 }
        ).awaitAsOne()
    }

    fun insertExpanded(
        parentCommentId: ItemId,
        expanded: Boolean,
        children: List<HnItem.Comment>,
    ) {
        if (!expanded) return
        db.transaction {
            db.updateExpanded(
                id = parentCommentId, expanded = if (expanded) 1 else 0
            ).executeAsOne()
            children.fastForEachIndexed { i, item ->
                db.insertComment(item.toEntity(i))
            }
        }
    }
}