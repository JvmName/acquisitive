package dev.jvmname.acquisitive.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dev.jvmname.acquisitive.network.adapters.IdAdapter
import dev.jvmname.acquisitive.network.adapters.InstantAdapter
import kotlinx.datetime.Instant
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.reflect.KClass

@ContributesTo(AppScope::class)
interface NetworkComponent {
    @[Provides SingleIn(AppScope::class)]
    fun providesMoshi(): Moshi {
        return Moshi.Builder()
            .add(Instant::class.java, InstantAdapter)
            .add(IdAdapter.create())
            .build()
    }

    @Provides
    fun provideMoshiConverterFactory(moshi: Moshi) = MoshiConverterFactory.create(moshi)

@Provides
fun provideOkhttpClient(): OkHttpClient {
return OkHttpClient.Builder()
    .build()
}


    @[Inject SingleIn(AppScope::class)]
    class RetrofitFactory(
        private val okhttp: () -> OkHttpClient,
        private val moshiConverterFactory: MoshiConverterFactory,
    ) {
        fun <T : Any> create(baseURL: String, clazz: KClass<T>): T {
            return Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(moshiConverterFactory)
                .validateEagerly(true)
                .callFactory { request -> okhttp().newCall(request) }
                .build()
                .create(clazz.java)
        }

        inline fun <reified T : Any> create(baseURL: String): T = create(baseURL, T::class)
    }
}