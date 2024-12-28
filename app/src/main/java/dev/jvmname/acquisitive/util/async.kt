package dev.jvmname.acquisitive.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


//https://androidstudygroup.slack.com/archives/C03MHQ3NU/p1666367020309989
suspend fun <T, R> Collection<T>.fetchAsync(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    transform: suspend (T) -> R,
): List<R> = coroutineScope {
    map {
        async(dispatcher) {
            transform(it)
        }
    }.awaitAll()
}
