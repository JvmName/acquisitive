package dev.jvmname.acquisitive.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import com.slack.circuit.retained.rememberRetained
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import logcat.asLog
import logcat.logcat
import java.io.IOException
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun String.capitalize(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault())
    else it.toString()
}


//https://androidstudygroup.slack.com/archives/C03MHQ3NU/p1666367020309989
suspend fun <T, R> Collection<T>.fetchAsync(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    transform: suspend (T) -> R,
): List<R> = coroutineScope {
    map {
        async(dispatcher + CoroutineName("fetchAsync")) {
            transform(it)
        }
    }.awaitAll()
}

// https://stackoverflow.com/a/46890009
suspend fun <T> retry(
    times: Int = Int.MAX_VALUE,
    initialDelay: Duration = 0.1.seconds,
    maxDelay: Duration = 1.seconds,
    factor: Double = 2.0,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: IOException) {
            logcat(tag = "retry") { "Error while retrying: " + e.asLog() }
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).coerceAtMost(maxDelay)
    }
    return block() // last attempt
}

// https://chrisbanes.me/posts/retaining-beyond-viewmodels/
@Composable
fun rememberRetainedCoroutineScope(): CoroutineScope {
    return rememberRetained("coroutine_scope") {
        object : RememberObserver {
            val scope = CoroutineScope(Dispatchers.Main + Job())

            override fun onForgotten() {
                // We've been forgotten, cancel the CoroutineScope
                scope.cancel()
            }

            // Not called by Circuit
            override fun onAbandoned() = Unit

            // Nothing to do here
            override fun onRemembered() = Unit
        }
    }.scope
}