package dev.jvmname.acquisitive.ui.screen.commentlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.jvmname.acquisitive.repo.HnItemRepository
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope



@[Composable CircuitInject(CommentListScreen::class, AppScope::class)]
fun CommentListContent(state: CommentListScreen.CommentListState, modifier: Modifier){
    TODO()
}