package com.remindly.app.core.domain.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.remindly.app.core.data.RemindlyDatabase
import com.remindly.app.core.domain.model.Reminder
import com.remindly.app.core.domain.model.ReminderStatus
import com.remindly.app.core.domain.model.RepeatRule
import com.remindly.app.core.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Handles notification action buttons (PRD §36):
 *   DONE   → mark that occurrence completed
 *   SNOOZE → schedule a new local notification (default 10 min)
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DONE = "com.remindly.app.action.DONE"
        const val ACTION_SNOOZE = "com.remindly.app.action.SNOOZE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_OCCURRENCE = "occurrence_utc"
        const val EXTRA_OCCURRENCE_LOCAL = "occurrence_local"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        private const val TAG = "NotifActionReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (id < 0) return
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_DONE -> done(context, id, intent)
                    ACTION_SNOOZE -> snooze(context, id, intent)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Notification action failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun done(context: Context, id: Long, intent: Intent) {
        val zone = ZoneId.systemDefault()
        val localRaw = intent.getStringExtra(EXTRA_OCCURRENCE_LOCAL)
        val occurrence = localRaw?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            ?: LocalDateTime.now()

        val db = RemindlyDatabase.build(context)
        val dao = db.reminderDao()
        // Mark occurrence completed (recurring) and/or flip one-shot status
        dao.markCompleted(
            com.remindly.app.core.data.entity.ReminderCompletionEntity(
                reminderId = id,
                occurrenceAt = occurrence.atZone(zone).toInstant().toEpochMilli(),
            )
        )
        val entity = dao.getById(id)
        if (entity != null && entity.recurrence == "NONE") {
            dao.updateStatus(id, ReminderStatus.COMPLETED.name)
        }

        NotificationHelper(context).cancel(id, occurrence)

        // Advance recurring reminders to the next occurrence (§56)
        if (entity != null) {
            val rule = RepeatRule.parse(entity.recurrence)
            if (rule.freq != RepeatRule.NONE.freq) {
                val start = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity.startAt), zone)
                val next = com.remindly.app.core.domain.recurrence.RecurrenceEngine
                    .nextOccurrence(rule, start, occurrence.plusNanos(1))
                if (next != null) {
                    ReminderScheduler(context).scheduleAt(id, next)
                }
            }
        }
        com.remindly.app.features.more.widget.WidgetUpdater.update(context)
    }

    private fun snooze(context: Context, id: Long, intent: Intent) {
        val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10).coerceIn(1, 24 * 60)
        val localRaw = intent.getStringExtra(EXTRA_OCCURRENCE_LOCAL)
        val base = localRaw?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            ?: LocalDateTime.now()
        val snoozedTo = LocalDateTime.now().plusMinutes(minutes.toLong())
        // Cancel the original firing's notification, then re-post after delay
        NotificationHelper(context).cancel(id, base)
        ReminderScheduler(context).scheduleAt(id, snoozedTo)
    }
}
