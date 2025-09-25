package dev.jvmname.acquisitive.repo.comment

import dev.drewhamilton.poko.Poko
import dev.jvmname.acquisitive.db.CommentEntity
import dev.jvmname.acquisitive.db.ObserveComments
import dev.jvmname.acquisitive.network.model.HnItem

@Poko
class RankedComment(
    val comment: HnItem.Comment,
    val depth: Int,
    val rank: Int,
    val expanded: Boolean,
)

fun HnItem.Comment.toEntity(rank: Int): CommentEntity {
    return CommentEntity(
        id = id,
        storyId = id,
        rank = rank,
        author = by,
        time = time,
        dead = dead,
        deleted = deleted,
        kids = kids,
        text = text,
        parent = parent,
        expanded = false
    )
}

fun ObserveComments.toEntity(): CommentEntity {
    return CommentEntity(
        id = id,
        storyId = storyId,
        parent = parent,
        author = author,
        time = time,
        dead = dead,
        deleted = deleted,
        kids = kids,
        text = text,
        rank = rank,
        expanded = expanded,
    )
}