package dev.jvmname.acquisitive.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.util.fastMapIndexedNotNull
import dev.jvmname.acquisitive.db.CommentEntity
import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.repo.comment.toEntity

@Composable
fun String.capitalize(): String = toLowerCase(Locale.current).capitalize(Locale.current)

fun List<HnItem>.toCommentEntity(): List<CommentEntity> = fastMapIndexedNotNull { i, comment ->
    (comment as? HnItem.Comment)?.toEntity(
        rank = i,
        expanded = false
    )
}