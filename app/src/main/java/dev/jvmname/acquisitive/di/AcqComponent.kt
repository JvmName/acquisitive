package dev.jvmname.acquisitive.di

import android.content.Context
import com.slack.circuit.foundation.Circuit
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@[MergeComponent(AppScope::class) SingleIn(AppScope::class)]
abstract class AcqComponent(
    @get:Provides @AppContext val context: Context,
    @get:Provides @AppCrScope val coroutineScope: CoroutineScope,
) {
    abstract val circuit: Circuit
}

@Qualifier
annotation class AppContext

@Qualifier
annotation class AppCrScope