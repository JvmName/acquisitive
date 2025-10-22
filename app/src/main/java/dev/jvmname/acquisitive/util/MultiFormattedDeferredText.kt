  package dev.jvmname.acquisitive.util

import android.content.Context
import androidx.compose.runtime.Stable
import com.backbase.deferredresources.DeferredFormattedString
import com.backbase.deferredresources.DeferredText
import com.backbase.deferredresources.text.FormattedDeferredText
import com.backbase.deferredresources.text.ParcelableDeferredText
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

fun DeferredFormattedString.withMultiFormatArgs(vararg formatArgs: Any): DeferredText =
    MultiFormattedDeferredText(wrapped = this, formatArgs = formatArgs)

@[Parcelize Stable]
class MultiFormattedDeferredText(
    private val wrapped: @RawValue DeferredFormattedString,
    private val formatArgs: @RawValue List<Any>,
) : ParcelableDeferredText {

    constructor(
        wrapped: DeferredFormattedString,
        vararg formatArgs: Any,
    ) : this(wrapped, formatArgs.toList())

    override fun resolve(context: Context): CharSequence {
        val resolved = formatArgs.map {
            when (it) {
                is FormattedDeferredText -> it.resolve(context)
                is DeferredText -> it.resolve(context)
                else -> it
            }
        }
        return wrapped.resolve(context, *resolved.toTypedArray())
    }


}