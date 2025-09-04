package dev.jvmname.acquisitive.di

import android.content.Context
import coil3.ImageLoader
import com.slack.circuit.foundation.Circuit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import kotlinx.coroutines.CoroutineScope


@DependencyGraph(scope = AppScope::class)
interface AcqGraph {
    val circuit: Circuit
    val imageLoader: ImageLoader

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides @AppContext context: Context,
            @Provides @AppCoroutineScope coroutineScope: CoroutineScope,
        ): AcqGraph
    }
}

@Qualifier
annotation class AppContext

@Qualifier
annotation class AppCoroutineScope