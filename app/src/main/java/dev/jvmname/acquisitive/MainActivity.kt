package dev.jvmname.acquisitive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuitx.android.rememberAndroidScreenAwareNavigator
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import com.theapache64.rebugger.RebuggerConfig
import dev.jvmname.acquisitive.di.AcqGraph
import dev.jvmname.acquisitive.ui.screen.mainlist.MainListScreen
import dev.jvmname.acquisitive.ui.theme.AcquisitiveTheme
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.logcat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidLogcatLogger.installOnDebuggableApp(application, minPriority = LogPriority.VERBOSE)
        RebuggerConfig.init(
            tag = "AcqRebugger", // changing default tag
            logger = { tag, message -> logcat(tag = tag, message = { message }) }
        )
        System.setProperty("kotlinx.coroutines.debug", if (BuildConfig.DEBUG) "on" else "off")
        enableEdgeToEdge()
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        val graph = createGraphFactory<AcqGraph.Factory>()
            .create(context = applicationContext, coroutineScope = scope)

        logcat { "***entering compose" }
        setContent {
            CircuitCompositionLocals(graph.circuit) {
                AcquisitiveTheme {
                    logcat { "***entered theme" }
                    val backstack = rememberSaveableBackStack(root = MainListScreen())
                    val navigator = rememberAndroidScreenAwareNavigator(
                        rememberCircuitNavigator(backstack), // Decorated navigator
                        this@MainActivity,
                    )
                    NavigableCircuitContent(
                        navigator = navigator,
                        backStack = backstack,
                        decoratorFactory = remember(navigator){
                            GestureNavigationDecorationFactory(
                                onBackInvoked = navigator::pop,
                            )
                        },
                    )
                }
            }
        }
    }
}