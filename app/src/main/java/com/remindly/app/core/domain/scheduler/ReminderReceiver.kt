package com.remindly.app.core.domain.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.remindly.app.core.data.RemindlyDatabase
import com.remindly.app.core.domain.model.Priority
import com.remindly.app.core.domain.model.Reminder
import com.remindly.app.core.domain.model.ReminderStatus
import com.remindly.app.core.domain.model.RepeatRule
import com.remindly.app.core.domain.recurrence.RecurrenceEngine
import com.remindly.app.core.notifications.NotificationHelper
import com.remindly.app.features.more.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Receives AlarmManager fires (PRD §44–46, §56):
 *   Notification fires → mark occurrence delivered → calculate next → schedule next
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE = "com.remindly.app.action.FIRE"
        const val ACTION_CANCEL_REMINDER = "com.remindly.app.action.CANCEL_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_AT = "at"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESC = "desc"
        const val EXTRA_START = "start"
        const val EXTRA_RULE = "rule"
        const val EXTRA_SNOOZE_MIN = "snooze_min"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_VIBRATE = "vibrate"
        private const val TAG = "ReminderReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CANCEL_REMINDER -> {
                val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                if (id >= 0) ReminderScheduler(context).cancelReminder(id)
            }
            ACTION_FIRE -> {
                val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                val atRaw = intent.getStringExtra(EXTRA_AT) ?: return
                val at = runCatching { LocalDateTime.parse(atRaw) }.getOrNull() ?: return
                val pending = goAsync()
                scope.launch {
                    try {
                        fire(context, id, at, intent)
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to fire reminder $id", t)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private suspend fun fire(context: Context, id: Long, at: LocalDateTime, intent: Intent) {
        val db = RemindlyDatabase.build(context)
        val dao = db.reminderDao()
        val entity = dao.getById(id) ?: return
        if (entity.deletedAt != null || entity.status == ReminderStatus.ARCHIVED.name) return

        val rule = RepeatRule.parse(entity.recurrence)
        val start = com.remindly.app.core.domain.model.Reminder(
            id = id,
            title = entity.title,
            startAt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity.startAt), java.time.ZoneId.systemDefault()),
            timezone = entity.timezone,
            rule = rule,
        ).startAt

        val reminder = Reminder(
            id = id,
            title = intent.getStringExtra(EXTRA_TITLE) ?: entity.title,
            description = intent.getStringExtra(EXTRA_DESC) ?: entity.description,
            category = entity.category,
            priority = runCatching { Priority.valueOf(entity.priority) }.getOrDefault(Priority.NORMAL),
            startAt = start,
            timezone = entity.timezone,
            rule = rule,
            status = ReminderStatus.ACTIVE,
            soundEnabled = intent.getBooleanExtra(EXTRA_SOUND, entity.soundEnabled),
            vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATE, entity.vibrationEnabled),
            snoozeEnabled = entity.snoozeEnabled,
            snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MIN, entity.snoozeMinutes),
        )

        val helper = NotificationHelper(context)
        helper.showReminder(reminder, at)

        // Schedule the following occurrence (§56 rolling window)
        val scheduler = ReminderScheduler(context)
        val next = RecurrenceEngine.nextOccurrence(rule, start, at.plusNanos(1))
        if (next != null && rule.freq != RepeatRule.NONE.freq) {
            scheduler.scheduleAt(id, next, reminder)
        } else if (rule.freq == RepeatRule.NONE.freq) {
            // One-shot: leave status ACTIVE until user completes; no re-arm.
            scheduler.cancelReminder(id)
        }
        WidgetUpdater.update(context)
    }
}
