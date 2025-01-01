package dev.jvmname.acquisitive.ui.screen.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dev.jvmname.acquisitive.network.model.FetchMode
import dev.jvmname.acquisitive.ui.types.HnScreenItem
import kotlinx.parcelize.Parcelize
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

private val MIN_ITEM_HEIGHT = 200.dp

@Parcelize
data class MainScreen(val fetchMode: FetchMode = FetchMode.TOP) : Screen {

    data class MainState(val stories: List<HnScreenItem>) : CircuitUiState
}

@[Composable CircuitInject(MainScreen::class, AppScope::class)]
fun MainUi(state: MainScreen.MainState, modifier: Modifier = Modifier){

}


@Composable
fun MainListItem(
    state: MainScreen.MainState,
    modifier: Modifier,
) {
    //TODO size, color, etc.
    /*  OutlinedCard(modifier = modifier) {
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

      }*/
}


//@[Preview Composable]
//fun PreviewMainListItem() {
//    MainListItem(
//        title = "Archimedes, Vitruvius, and Leonardo: The Odometer Connection (2020)",
//        isHot = false,
//        rank = 2,
//        score = 950,
//        numChildren = 100,
//        time = "19h",
//        author = "JvmName",
//        icon = null,
//        urlHost = "github.com"
//    )
//}