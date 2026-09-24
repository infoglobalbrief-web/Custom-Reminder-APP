package com.remindly.app.core.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.remindly.app.core.data.RemindlyDatabase
import com.remindly.app.core.domain.model.Reminder
import com.remindly.app.core.domain.model.ReminderStatus
import com.remindly.app.core.domain.recurrence.RecurrenceEngine
import com.remindly.app.core.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Dedicated ReminderScheduler service (PRD §55–56, §45 critical rule):
 *
 *   Phone → Local reminder DB → Native OS scheduler → Alarm/Notification
 *
 * Backend is NEVER consulted to fire a reminder — offline-first (§38).
 *
 * Strategy: schedule only the *next* occurrence per reminder. When it
 * fires, mark delivered and schedule the following one (§56).
 */
class ReminderScheduler(private val context: Context) {

    companion object {
        private const val TAG = "ReminderScheduler"
        private const val MIN_TRIGGER_MS = 1_500L // ignore sub-2s races

        fun actionRequestCode(reminderId: Long, at: LocalDateTime, action: String): Int {
            var h = reminderId.hashCode() * 31 + at.toLocalDate().toEpochDay().hashCode()
            h = h * 31 + (at.hour * 60 + at.minute)
            h = h * 31 + action.hashCode()
            return h
        }
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    // ------------------------------------------------------------------
    // Public API (PRD §55)
    // ------------------------------------------------------------------

    /** schedule() — arm the next occurrence of one reminder. */
    fun schedule(reminder: Reminder, after: LocalDateTime = LocalDateTime.now()) {
        val next = RecurrenceEngine.nextOccurrence(reminder.rule, reminder.startAt, after) ?: run {
            cancelReminder(reminder.id)
            return
        }
        scheduleAt(reminder.id, next, reminder)
    }

    /** cancel() for a single firing. */
    fun cancel(reminderId: Long, at: LocalDateTime) {
        alarmManager.cancel(pendingFire(reminderId, at, create = false))
    }

    /** cancelReminder() — drop every pending fire for this reminder. */
    fun cancelReminder(reminderId: Long) {
        // Tagged cancel via explicit broadcast intent (stable request code per reminder)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_CANCEL_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            tagCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pi)
        pi.cancel()
    }

    /** reschedule() — re-arm a reminder at an explicit time (snooze). */
    fun scheduleAt(reminderId: Long, at: LocalDateTime, reminder: Reminder? = null) {
        val triggerAt = at.atZone(zone).toInstant().toEpochMilli()
        val delay = triggerAt - System.currentTimeMillis()
        if (delay < -60_000 && reminder?.rule?.freq == com.remindly.app.core.domain.model.Freq.NONE) {
            return // missed one-shot; MissedNotificationHandler deals with it
        }
        val pi = pendingFire(reminderId, at, create = true, reminder = reminder)
        try {
            when {
                delay < MIN_TRIGGER_MS -> {
                    // Fire imminently
                    if (canScheduleExact()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + MIN_TRIGGER_MS, pi)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + MIN_TRIGGER_MS, pi)
                    }
                }
                canScheduleExact() -> alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                else -> alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (se: SecurityException) {
            // Exact-alarm permission revoked (PRD §38 permission changes)
            Log.w(TAG, "Exact alarm denied — falling back to inexact", se)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /**
     * rebuildSchedules() — called on app launch, boot, timezone change
     * and package update (PRD §38, §57).
     */
    fun rebuildSchedules() {
        scope.launch {
            try {
                val db = RemindlyDatabase.build(context)
                val dao = db.reminderDao()
                val helper = NotificationHelper(context)
                helper.ensureChannels()
                for (entity in dao.getActive()) {
                    if (entity.deletedAt != null) continue
                    val start = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity.startAt), zone)
                    val reminder = Reminder(
                        id = entity.id,
                        title = entity.title,
                        description = entity.description,
                        category = entity.category,
                        priority = com.remindly.app.core.domain.model.Priority.valueOf(entity.priority),
                        startAt = start,
                        timezone = entity.timezone,
                        rule = com.remindly.app.core.domain.model.RepeatRule.parse(entity.recurrence),
                        status = ReminderStatus.ACTIVE,
                        soundEnabled = entity.soundEnabled,
                        vibrationEnabled = entity.vibrationEnabled,
                        snoozeEnabled = entity.snoozeEnabled,
                        snoozeMinutes = entity.snoozeMinutes,
                    )
                    // Skip occurrences already completed
                    var next = RecurrenceEngine.nextOccurrence(reminder.rule, reminder.startAt, LocalDateTime.now())
                    val completions = dao.completionsFor(entity.id).map { it.occurrenceAt }.toSet()
                    while (next != null &&
                        next.atZone(zone).toInstant().toEpochMilli() in completions &&
                        (next.atZone(zone).toInstant().toEpochMilli()) < System.currentTimeMillis() + 86_400_000
                    ) {
                        next = RecurrenceEngine.nextOccurrence(reminder.rule, reminder.startAt, next.plusNanos(1))
                    }
                    if (next != null) scheduleAt(reminder.id, next, reminder)
                }
                // People birthdays → materialized as one-shot window reminders? (MVP:
                // birthday alerts are scheduled lazily via nextBirthdayAlert when a
                // person is saved — see PeopleViewModel.)
            } catch (t: Throwable) {
                Log.e(TAG, "rebuildSchedules failed", t)
            }
        }
    }

    /** scheduleNextOccurrence() — after a fire, arm the following one (§56). */
    fun scheduleNextOccurrence(reminder: Reminder, after: LocalDateTime) {
        schedule(reminder, after)
    }

    fun handleDeviceReboot() = rebuildSchedules()
    fun handleTimezoneChange() = rebuildSchedules()

    // ------------------------------------------------------------------

    private fun tagCode(reminderId: Long): Int = (reminderId * 7919L).toInt()

    private fun pendingFire(
        reminderId: Long,
        at: LocalDateTime,
        create: Boolean,
        reminder: Reminder? = null,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_AT, at.toString())
            reminder?.let {
                putExtra(ReminderReceiver.EXTRA_TITLE, it.title)
                putExtra(ReminderReceiver.EXTRA_DESC, it.description)
                putExtra(ReminderReceiver.EXTRA_START, it.startAt.toString())
                putExtra(ReminderReceiver.EXTRA_RULE, it.rule.serialize())
                putExtra(ReminderReceiver.EXTRA_SNOOZE_MIN, it.snoozeMinutes)
                putExtra(ReminderReceiver.EXTRA_SOUND, it.soundEnabled)
                putExtra(ReminderReceiver.EXTRA_VIBRATE, it.vibrationEnabled)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            actionRequestCode(reminderId, at, "FIRE"),
            intent,
            (if (create) PendingIntent.FLAG_UPDATE_CURRENT else 0) or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
