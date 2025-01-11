package dev.jvmname.acquisitive.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import logcat.asLog
import logcat.logcat
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


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