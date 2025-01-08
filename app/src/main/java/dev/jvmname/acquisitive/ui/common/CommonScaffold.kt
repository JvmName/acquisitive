package dev.jvmname.acquisitive.ui.common

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.retained.rememberRetained
import dev.jvmname.acquisitive.ui.theme.AcquisitiveTheme
import kotlinx.collections.immutable.persistentListOf
/*

@Composable
fun foo(modifier: Modifier = Modifier) {
    var contentComposed by rememberRetained { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxWidth(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        bottomBar = {
            AcquisitiveTheme {
                Layout(
                    modifier = Modifier,
                    measurePolicy = { measurables, constraints ->
                        val placeable = measurables.first().measure(constraints)
                        layout(
                            placeable.width,
                            placeable.height
                        ) { placeable.place(IntOffset.Zero) }
                    },
                    content = {
                        BottomNavigationBar(
                            selectedIndex = state.selectedIndex,
                            onSelectedIndex = { index -> state.eventSink(ClickNavItem(index)) },
                        )
                    },
                )
            }
        },
    ) { paddingValues ->
        contentComposed = true
        val screen = state.navItems[state.selectedIndex].screen
        CircuitContent(
            screen,
            modifier = Modifier.padding(paddingValues),
            onNavEvent = { event -> state.eventSink(ChildNav(event)) },
        )
    }
}

@Composable
private fun BottomNavigationBar(
    selectedIndex: Int,
    onSelectedIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        NAV_ITEMS.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                alwaysShowLabel = true,
                selected = selectedIndex == index,
                onClick = { onSelectedIndex(index) },
            )
        }
    }
}*/
