package com.remindly.app.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.remindly.app.MainActivity
import com.remindly.app.R
import com.remindly.app.core.domain.model.Reminder
import com.remindly.app.core.domain.scheduler.NotificationActionReceiver
import com.remindly.app.core.domain.scheduler.ReminderScheduler
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Notification channels + rich notification layout (PRD §35–36).
 * Tapping → deep link → Reminder Detail. Actions: DONE · SNOOZE.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_BIRTHDAYS = "birthdays"
        private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    }

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_reminders_desc)
                setSound(sound, attrs)
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BIRTHDAYS,
                context.getString(R.string.channel_birthdays),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_birthdays_desc)
                setSound(sound, attrs)
                enableVibration(true)
            }
        )
    }

    fun areNotificationsEnabled(): Boolean {
        return androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Build and post the firing notification.
     * @param isBirthday switches channel + copy (PRD §35 birthday style).
     */
    fun showReminder(
        reminder: Reminder,
        occurrenceAt: LocalDateTime,
        isBirthday: Boolean = false,
        birthdayDaysLeft: Int? = null,
    ) {
        ensureChannels()
        if (!areNotificationsEnabled()) return

        val title = if (isBirthday) {
            when (birthdayDaysLeft) {
                0 -> "${reminder.title} — today!"
                1 -> "${reminder.title} is tomorrow."
                else -> "${reminder.title} in ${birthdayDaysLeft ?: 0} days."
            }
        } else reminder.title

        val text = when {
            isBirthday -> "${occurrenceAt.toLocalDate()} · tap for details"
            reminder.description.isNotBlank() -> reminder.description
            else -> occurrenceAt.format(timeFmt)
        }

        // Deep link → Reminder Detail (PRD §36)
        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            MainActivity.deeplinkIntent(context, reminder.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val doneIntent = PendingIntent.getBroadcast(
            context,
            ReminderScheduler.actionRequestCode(reminder.id, occurrenceAt, action = "DONE"),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_DONE
                putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminder.id)
                putExtra(NotificationActionReceiver.EXTRA_OCCURRENCE, occurrenceAt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli())
                putExtra(NotificationActionReceiver.EXTRA_OCCURRENCE_LOCAL, occurrenceAt.toString())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            ReminderScheduler.actionRequestCode(reminder.id, occurrenceAt, action = "SNZ"),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_SNOOZE
                putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminder.id)
                putExtra(NotificationActionReceiver.EXTRA_OCCURRENCE_LOCAL, occurrenceAt.toString())
                putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, reminder.snoozeMinutes)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, if (isBirthday) CHANNEL_BIRTHDAYS else CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setColor(context.getColor(R.color.purple_primary))

        if (reminder.soundEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }
        if (reminder.vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 250, 150, 250))
        }

        if (!isBirthday) {
            builder.addAction(0, context.getString(R.string.action_done), doneIntent)
            if (reminder.snoozeEnabled) {
                builder.addAction(0, context.getString(R.string.action_snooze), snoozeIntent)
            }
        } else {
            builder.addAction(0, context.getString(R.string.action_done), doneIntent)
        }

        nm.notify(reminderNotificationId(reminder.id, occurrenceAt), builder.build())
        vibrateIfEnabled(reminder.vibrationEnabled)
    }

    fun cancel(reminderId: Long, occurrenceAt: LocalDateTime) {
        nm.cancel(reminderNotificationId(reminderId, occurrenceAt))
    }

    fun cancelAllForReminder(reminderId: Long) {
        // Best-effort: active notifications use deterministic ids.
        nm.cancel(reminderNotificationId(reminderId, LocalDateTime.MIN))
    }

    private fun reminderNotificationId(reminderId: Long, at: LocalDateTime): Int =
        (reminderId * 31 + at.toLocalDate().toEpochDay()).toInt()

    @Suppress("DEPRECATION")
    private fun vibrateIfEnabled(enabled: Boolean) {
        if (!enabled) return
        val pattern = longArrayOf(0, 250, 150, 250)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }
}
