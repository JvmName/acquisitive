package dev.jvmname.acquisitive.repo

import android.content.Context
import com.mercury.sqkon.db.Sqkon
import dev.jvmname.acquisitive.di.AppContext
import dev.jvmname.acquisitive.di.AppCrScope
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesTo(AppScope::class)
interface SqkonComponent {

    @[Provides SingleIn(AppScope::class)]
    fun provideSqkon(
        @AppContext context: Context,
        @AppCrScope coroutineScope: CoroutineScope,
    ): Sqkon {
        return Sqkon(
            context = context,
            scope = coroutineScope
        )
    }
}