package com.remindly.app.core.di

import android.content.Context
import com.remindly.app.core.data.RemindlyDatabase
import com.remindly.app.core.data.repo.PeopleRepository
import com.remindly.app.core.data.repo.ReminderRepository
import com.remindly.app.core.data.repo.SettingsRepository
import com.remindly.app.core.domain.scheduler.ReminderScheduler
import com.remindly.app.core.notifications.NotificationHelper

/**
 * Manual DI container (lightweight AppContainer — PRD §41 architecture).
 * ViewModels resolve dependencies from [com.remindly.app.RemindlyApp.container].
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: RemindlyDatabase by lazy { RemindlyDatabase.build(appContext) }
    val reminderRepository: ReminderRepository by lazy { ReminderRepository(database) }
    val peopleRepository: PeopleRepository by lazy { PeopleRepository(database) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val notificationHelper: NotificationHelper by lazy { NotificationHelper(appContext) }
    val scheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }
}
