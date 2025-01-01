package dev.jvmname.acquisitive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuitx.android.rememberAndroidScreenAwareNavigator
import com.slack.circuitx.gesturenavigation.GestureNavigationDecoration
import dev.jvmname.acquisitive.di.AcqComponent
import dev.jvmname.acquisitive.di.create
import dev.jvmname.acquisitive.ui.screen.main.MainScreen
import dev.jvmname.acquisitive.ui.theme.AcquisitiveTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        val component = AcqComponent::class.create(
            contextDelegate = applicationContext,
            coroutineScopeDelegate = scope,
        )

        setContent {
            CircuitCompositionLocals(component.circuit) {
                AcquisitiveTheme {
                    val backstack = rememberSaveableBackStack(root = MainScreen())
                    val navigator = rememberAndroidScreenAwareNavigator(
                        rememberCircuitNavigator(backstack), // Decorated navigator
                        this@MainActivity,
                    )
                    NavigableCircuitContent(
                        navigator = navigator,
                        backStack = backstack,
                        decoration = GestureNavigationDecoration(
                            fallback = NavigatorDefaults.DefaultDecoration,
                            onBackInvoked = navigator::pop
                        )
                    )
                }
            }
        }
    }
}