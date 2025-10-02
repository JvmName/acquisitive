package dev.jvmname.acquisitive.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jvmname.acquisitive.ui.types.Favicon
import dev.jvmname.acquisitive.ui.types.HnScreenItem

@Composable
fun Favicon(story: HnScreenItem.Story) {
    when (val favicon = story.favicon) {
        is Favicon.Icon -> AsyncImage(
            model = favicon.url,
            contentDescription = "favicon",
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape),
            fallback = rememberVectorPainter(Icons.Default.Public)
        )

        is Favicon.Default -> Icon(
            favicon.vector,
            "",
            Modifier.size(12.dp)
        )
    }
}