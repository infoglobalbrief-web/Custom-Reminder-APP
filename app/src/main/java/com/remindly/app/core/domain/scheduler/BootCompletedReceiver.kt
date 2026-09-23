package com.remindly.app.core.domain.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.remindly.app.features.more.widget.WidgetUpdater

/**
 * Reliability receiver (PRD §38, §57):
 *   BOOT / MY_PACKAGE_REPLACED / TIME_SET / TIMEZONE_CHANGED
 *   → load active reminders → calculate next occurrence → reschedule
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            -> {
                ReminderScheduler(context).handleDeviceReboot()
                WidgetUpdater.update(context)
            }
        }
    }
}
