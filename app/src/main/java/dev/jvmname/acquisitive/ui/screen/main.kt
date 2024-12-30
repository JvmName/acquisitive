package dev.jvmname.acquisitive.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.TextButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val MIN_ITEM_HEIGHT = 200.dp


@Composable
fun MainListItem(
    modifier: Modifier = Modifier,
    title: String,
    isHot: Boolean,
    rank: Int,
    score: Int,
    urlHost: String,
    numChildren: Int,
    time: String,
    author: String,
    icon: ImageVector?,
    onItemClick: ()
) {
    //TODO size, color, etc.
    OutlinedCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.Start) {

            //ranking
            Column(
                modifier.size(width = 150.dp, height = MIN_ITEM_HEIGHT),
                verticalArrangement = Arrangement.Center
            ) {
                Text(rank.toString())
                Text(score.toString())
            }

            //main "content" -- TODO this should prob be a constraintlayout
            Column(
                modifier
                    .heightIn(min = MIN_ITEM_HEIGHT)
                    .wrapContentWidth()
            ) {
                Text(title)
                Text(urlHost)

                Row {
                    Text(
                        "$time - $author", //would probably want this sent by VM
                        modifier.weight(1f)
                    )

                    TextButton(modifier = modifier.weight(1f),
                        onClick = {},
                        content = { })
                }


            }
        }

    }
}


@[Preview Composable]
fun PreviewMainListItem() {
    MainListItem(
        title = "Archimedes, Vitruvius, and Leonardo: The Odometer Connection (2020)",
        isHot = false,
        rank = 2,
        score = 950,
        numChildren = 100,
        time = "19h",
        author = "JvmName",
        icon = null
    )
}