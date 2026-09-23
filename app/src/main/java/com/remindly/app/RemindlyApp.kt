package com.remindly.app

import android.app.Application
import com.remindly.app.core.di.AppContainer
import com.remindly.app.core.domain.scheduler.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry — PRD §57 App lifecycle:
 *   App launch → Load DB → Check scheduler state → Repair missing schedules → Show Home
 */
class RemindlyApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.ensureChannels()
        appScope.launch {
            // Local-first bootstrap (PRD §38)
            container.reminderRepository.seedCategories()
            // Repair any schedules lost to process death / update (§57)
            container.scheduler.rebuildSchedules()
            container.settingsRepository.bumpLaunchCount()
        }
    }

    override fun onTerminate() {
        // Not called on real devices; kept for completeness.
        super.onTerminate()
    }
}
