package dev.jvmname.acquisitive.di

import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@[MergeComponent(AppScope::class) SingleIn(AppScope::class)]
abstract class AcqComponent