package dev.jvmname.acquisitive.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.addAdapter
import dev.jvmname.acquisitive.network.adapters.IdAdapter
import dev.jvmname.acquisitive.network.adapters.InstantAdapter
import logcat.logcat
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@ContributesTo(AppScope::class)
interface NetworkComponent {
    @[Provides SingleIn(AppScope::class)]
    fun providesMoshi(): Moshi {
        return Moshi.Builder()
            .addAdapter(InstantAdapter)
            .add(IdAdapter.create())
            .add(ItemIdArrayAdapterFactory)
            .build()
    }

    @Provides
    fun provideMoshiConverterFactory(moshi: Moshi) = MoshiConverterFactory.create(moshi)

    @Provides
    fun provideOkhttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addNetworkInterceptor(HttpLoggingInterceptor { logcat(tag = "OkhttpInterceptor") { it } }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .callTimeout(30.seconds.toJavaDuration())
            .readTimeout(30.seconds.toJavaDuration())
            .writeTimeout(30.seconds.toJavaDuration())
            .connectTimeout(30.seconds.toJavaDuration())
            .build()
    }


    @[Inject SingleIn(AppScope::class)]
    class RetrofitFactory(
        private val okhttp: OkHttpClient,
        private val moshiConverterFactory: MoshiConverterFactory,
    ) {
        fun <T : Any> create(baseURL: String, clazz: KClass<T>): T {
            return Retrofit.Builder()
                .client(okhttp)
                .baseUrl(baseURL)
                .addConverterFactory(moshiConverterFactory)
                .validateEagerly(true)
                .build()
                .create(clazz.java)
        }

        inline fun <reified T : Any> create(baseURL: String): T = create(baseURL, T::class)
    }
}