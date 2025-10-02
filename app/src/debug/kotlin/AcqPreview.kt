package dev.jvmname.acquisitive.dev

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.backbase.deferredresources.DeferredText
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.ui.theme.AcquisitiveTheme
import dev.jvmname.acquisitive.ui.theme.indent
import dev.jvmname.acquisitive.ui.types.Favicon
import dev.jvmname.acquisitive.ui.types.HnScreenItem

@Composable
fun AcqPreview(content: @Composable () -> Unit) {
    AcquisitiveTheme {
        content()
    }
}

fun previewStoryItem(id: Int): HnScreenItem.Story = HnScreenItem.Story(
    id = ItemId(id),
    title = "Archimedes, Vitruvius, and Leonardo: The Odometer Connection (2020)",
    isHot = true,
    score = "950",
    urlHost = "github.com",
    favicon = Favicon.Default(Icons.Default.Public),
    numChildren = 121 + id,
    author = DeferredText.Constant("JvmName - 19h"),
    time = "19h",
    isDeleted = false,
    isDead = false,
    titleSuffix = "💼",
    rank = "$id."
)

@Composable
fun previewComment(
    id: Int,
    depth: Int,
    rank: Int,
    expanded: Boolean = true,
): HnScreenItem.Comment {
    return HnScreenItem.Comment(
        id = ItemId(id),
        text = "This is a sample comment at depth $depth with some example text. Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        time = "${(1..12).random()}:${(10..59).random()} ago",
        author = DeferredText.Constant("Sample Author"),
        numChildren = if (depth < 3) (0..5).random() else 0,
        parent = ItemId(id - 1),
        indentDepth = (depth * 20).dp,
        indentColor = MaterialTheme.colorScheme.indent(depth),
        expanded = expanded,
        rank = rank
    )
}