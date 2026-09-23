package com.remindly.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.remindly.app.core.di.AppContainer
import com.remindly.app.core.data.repo.SettingsRepository
import com.remindly.app.ui.navigation.RemindlyNavHost
import com.remindly.app.ui.theme.RemindlyTheme

/**
 * Single-activity Compose host.
 * Splash (PRD §10) → Onboarding/Auth gate → Main scaffold with bottom
 * navigation + central floating + (PRD §9).
 */
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DEEPLINK_REMINDER_ID = "deeplink_reminder_id"

        /** Notification tap → Reminder Detail deep link (PRD §36). */
        fun deeplinkIntent(context: Context, reminderId: Long): Intent =
            Intent(
                Intent.ACTION_VIEW,
                android.net.Uri.parse("remindly://reminder?id=$reminderId"),
                context,
                MainActivity::class.java,
            ).putExtra(EXTRA_DEEPLINK_REMINDER_ID, reminderId)
    }

    private val container: AppContainer get() = (application as RemindlyApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deeplinkReminderId = intent?.getLongExtra(EXTRA_DEEPLINK_REMINDER_ID, -1L) ?: -1L

        setContent {
            RemindlyTheme {
                val settings by produceState<SettingsRepository.Settings?>(initialValue = null) {
                    value = container.settingsRepository.current()
                }
                val s = settings
                if (s == null) {
                    // Loading state (PRD §75) — brief, behind system splash
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {}
                } else {
                    RemindlyNavHost(
                        container = container,
                        settings = s,
                        deeplinkReminderId = deeplinkReminderId,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
