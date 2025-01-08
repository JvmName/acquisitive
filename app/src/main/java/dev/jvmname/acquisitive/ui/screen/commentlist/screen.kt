package dev.jvmname.acquisitive.ui.screen.commentlist

import androidx.compose.runtime.Composable
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.jvmname.acquisitive.repo.StoryItemRepo
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

@[Inject CircuitInject(CommentListScreen::class, AppScope::class)]
class CommentListPresenter(
    private val repo: StoryItemRepo,
    @Assisted private val screen: CommentListScreen,
    @Assisted private val navigator: Navigator,
) : Presenter<CommentListScreen.CommentListState> {
    @Composable
    override fun present(): CommentListScreen.CommentListState {
        TODO("Not yet implemented")
    }
}