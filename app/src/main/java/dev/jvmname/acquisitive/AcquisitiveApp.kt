package dev.jvmname.acquisitive

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dev.jvmname.acquisitive.di.AcqGraph
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AcquisitiveApp : Application(), SingletonImageLoader.Factory {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val graph by lazy {
        createGraphFactory<AcqGraph.Factory>()
            .create(context = applicationContext, coroutineScope = scope)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = graph.imageLoader
}