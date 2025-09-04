package dev.jvmname.acquisitive.di

import android.content.Context
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.network.CacheStrategy
import coil3.network.NetworkFetcher
import coil3.network.okhttp.asNetworkClient
import coil3.request.crossfade
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import okhttp3.OkHttpClient


@ContributesTo(AppScope::class)
interface CoilModule {

    @Provides
    fun providesCoilLoader(
        @AppContext context: Context,
        httpClient: Lazy<OkHttpClient>,
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(5242880) //5mb
                    .build()
            }
            .logger(null)
            .components {
                add(
                    NetworkFetcher.Factory(
                        { httpClient.value.asNetworkClient() },
                        CacheStrategy::DEFAULT
                    )
                )
            }
            .build()
    }
}