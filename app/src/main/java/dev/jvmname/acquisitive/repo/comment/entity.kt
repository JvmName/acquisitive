package dev.jvmname.acquisitive.repo.comment

import androidx.compose.runtime.Immutable
import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.db.CommentEntity
import dev.jvmname.acquisitive.network.model.HnItem

@[Poko Immutable]
class RankedComment(
    val comment: HnItem.Comment,
    val depth: Int,
    val rank: Int,
    val expanded: Boolean,
)

fun HnItem.Comment.toEntity(
    rank: Int,
    expanded: Boolean = false,
): CommentEntity {
    return CommentEntity(
        id = id,
        rank = rank,
        author = by,
        time = time,
        dead = dead,
        deleted = deleted,
        kids = kids,
        text = text,
        parent = parent,
        expanded = expanded
    )
}