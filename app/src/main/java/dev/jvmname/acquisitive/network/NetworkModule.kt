package dev.jvmname.acquisitive.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dev.jvmname.acquisitive.network.adapters.IdAdapter
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.reflect.KClass

@[Provides SingleIn(AppScope::class)]
fun providesMoshi(adapters: Set<JsonAdapter<*>>): Moshi {
    return Moshi.Builder()
        .apply { adapters.forEach(::add) }
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


@SingleIn(AppScope::class)
class RetrofitFactory @Inject constructor(
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