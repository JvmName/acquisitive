package dev.jvmname.acquisitive.domain

import android.content.Intent
import androidx.core.net.toUri
import com.slack.circuitx.android.IntentScreen
import dev.zacsweers.metro.Inject

@Inject
class IntentCreator {

    fun view(url: String): IntentScreen {
        return IntentScreen(Intent(Intent.ACTION_VIEW, url.toUri())) //todo KMP won't like this
    }

    fun share(title: String, body: String): IntentScreen {
        val inner = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        return IntentScreen(Intent.createChooser(inner, "Share this comment"))
    }
}