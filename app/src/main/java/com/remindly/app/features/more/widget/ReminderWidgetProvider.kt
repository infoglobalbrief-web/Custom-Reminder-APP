package com.remindly.app.features.more.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.remindly.app.MainActivity
import com.remindly.app.R
import com.remindly.app.core.data.RemindlyDatabase
import com.remindly.app.core.domain.recurrence.RecurrenceEngine
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Home-screen widget (PRD §34):
 *   Small — NEXT REMINDER · 10:00 PM · Daily Planning
 *   Medium — TODAY checklist (extension point)
 */
class ReminderWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            onUpdate(context, AppWidgetManager.getInstance(context),
                AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, ReminderWidgetProvider::class.java)))
        }
    }

    companion object {
        fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_reminder)
            val (title, timeText) = nextReminderSummary(context)
            views.setTextViewText(R.id.widget_label, context.getString(R.string.widget_next_reminder))
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_time, timeText)
            val tap = Intent(context, MainActivity::class.java)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                android.app.PendingIntent.getActivity(
                    context, 0, tap,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            return views
        }

        private fun nextReminderSummary(context: Context): Pair<String, String> = runBlocking {
            try {
                val db = RemindlyDatabase.build(context)
                val zone = java.time.ZoneId.systemDefault()
                val now = LocalDateTime.now()
                var bestTitle: String? = null
                var bestAt: LocalDateTime? = null
                for (entity in db.reminderDao().getActive()) {
                    if (entity.deletedAt != null) continue
                    val start = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity.startAt), zone)
                    val rule = com.remindly.app.core.domain.model.RepeatRule.parse(entity.recurrence)
                    val next = RecurrenceEngine.nextOccurrence(rule, start, now) ?: continue
                    if (bestAt == null || next.isBefore(bestAt)) {
                        bestAt = next
                        bestTitle = entity.title
                    }
                }
                if (bestTitle != null && bestAt != null) {
                    bestTitle to bestAt.format(DateTimeFormatter.ofPattern("h:mm a"))
                } else {
                    context.getString(R.string.widget_nothing) to "—"
                }
            } catch (t: Throwable) {
                context.getString(R.string.widget_nothing) to "—"
            }
        }
    }
}

/** Call after any reminder/birthday change so widgets stay fresh (PRD §34). */
object WidgetUpdater {
    fun update(context: Context) {
        try {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, ReminderWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = ReminderWidgetProvider.buildViews(context)
            for (id in ids) mgr.updateAppWidget(id, views)
        } catch (_: Throwable) {
            // Widgets are best-effort; never crash the scheduler path.
        }
    }
}
