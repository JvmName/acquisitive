package dev.jvmname.acquisitive.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@[MergeComponent(AppScope::class) SingleIn(AppScope::class)]
abstract class AcqComponent(
    @get:Provides @AppContext protected val context: Context,
    @get:Provides @AppCrScope protected val coroutineScope: CoroutineScope,
)

@Qualifier
annotation class AppContext

@Qualifier
annotation class AppCrScope