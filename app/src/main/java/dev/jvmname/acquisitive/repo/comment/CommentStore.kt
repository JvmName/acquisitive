package dev.jvmname.acquisitive.repo.comment

import androidx.compose.ui.util.fastForEachIndexed
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.jvmname.acquisitive.db.CommentEntity
import dev.jvmname.acquisitive.db.CommentQueries
import dev.jvmname.acquisitive.db.UpdateExpanded
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.util.mapList
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

@Inject
class CommentStore(private val db: CommentQueries) {
    fun refresh(parentId: ItemId, topLevelComments: List<HnItem.Comment>) {
        deletePreviousAndInsert(parentId, topLevelComments)
    }

    fun observeComments(parentId: ItemId): Flow<List<CommentEntity>> {
        return db.observeComments(parentId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .mapList { it.toEntity() }
    }

    suspend fun updateExpanded(id: ItemId): UpdateExpanded {
        return db.updateExpanded(id = id).awaitAsOne()
    }

    fun insertExpanded(
        parentCommentId: ItemId,
        expanded: Boolean,
        children: List<HnItem.Comment>,
    ) {
        if (expanded) {
            deletePreviousAndInsert(parentCommentId, children)
        }
    }

    private fun deletePreviousAndInsert(
        parentId: ItemId,
        topLevelComments: List<HnItem.Comment>,
    ) {
        db.transaction {
            db.deleteCommentsForParent(parentId)
            topLevelComments.fastForEachIndexed { i, item ->
                db.insertComment(item.toEntity(i))
            }
        }
    }
}