package com.remindly.app.core.data.repo

import com.remindly.app.core.data.RemindlyDatabase
import com.remindly.app.core.data.entity.ReminderAlertEntity
import com.remindly.app.core.data.entity.ReminderCompletionEntity
import com.remindly.app.core.data.entity.ReminderEntity
import com.remindly.app.core.domain.model.AlertSpec
import com.remindly.app.core.domain.model.Occurrence
import com.remindly.app.core.domain.model.Priority
import com.remindly.app.core.domain.model.Reminder
import com.remindly.app.core.domain.model.ReminderStatus
import com.remindly.app.core.domain.model.RepeatRule
import com.remindly.app.core.domain.recurrence.RecurrenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Single source of truth for reminders (PRD §41 Repository pattern,
 * §53 local-first change tracking).
 */
class ReminderRepository(private val db: RemindlyDatabase) {

    private val dao get() = db.reminderDao()
    val zone: ZoneId get() = ZoneId.systemDefault()

    fun observeReminders(): Flow<List<Reminder>> =
        dao.observeAll().map { list -> list.map { it.toDomain(emptyList()) } }

    fun search(query: String): Flow<List<Reminder>> =
        dao.search(query).map { list -> list.map { it.toDomain(emptyList()) } }

    fun observeCompletions(): Flow<List<ReminderCompletionEntity>> = dao.observeCompletions()

    suspend fun get(id: Long): Reminder? =
        dao.getById(id)?.toDomain(dao.alertsFor(id).map { it.toSpec() })

    suspend fun getAll(): List<Reminder> =
        dao.getAll().map { it.toDomain(dao.alertsFor(it.id).map { a -> a.toSpec() }) }

    suspend fun seedCategories() {
        if (dao.categoryCount() == 0) dao.insertCategories(RemindlyDatabase.DEFAULT_CATEGORIES)
    }

    suspend fun save(reminder: Reminder): Long {
        val now = System.currentTimeMillis()
        val entity = reminder.toEntity(now)
        val id = if (entity.id == 0L) dao.insert(entity) else {
            dao.update(entity.copy(updatedAt = now, dirty = true, syncVersion = entity.syncVersion + 1))
            entity.id
        }
        dao.replaceAlerts(
            id,
            reminder.alerts.ifEmpty { listOf(AlertSpec()) }.map {
                ReminderAlertEntity(reminderId = id, offsetMinutes = it.offsetMinutes, enabled = it.enabled)
            }
        )
        return id
    }

    suspend fun setStatus(id: Long, status: ReminderStatus) = dao.updateStatus(id, status.name)

    suspend fun delete(id: Long) = dao.softDelete(id)

    suspend fun duplicate(id: Long): Long? {
        val original = get(id) ?: return null
        return save(
            original.copy(
                id = 0,
                title = "${original.title} (copy)",
                status = ReminderStatus.ACTIVE,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    // ---- occurrences --------------------------------------------------

    suspend fun markCompleted(reminderId: Long, occurrenceAt: LocalDateTime) {
        dao.markCompleted(
            ReminderCompletionEntity(
                reminderId = reminderId,
                occurrenceAt = occurrenceAt.atZone(zone).toInstant().toEpochMilli(),
            )
        )
        val r = dao.getById(reminderId)
        if (r != null && r.recurrence == "NONE" && r.status == ReminderStatus.ACTIVE.name) {
            dao.updateStatus(reminderId, ReminderStatus.COMPLETED.name)
        }
    }

    suspend fun unmarkCompleted(reminderId: Long, occurrenceAt: LocalDateTime) {
        dao.unmarkCompleted(reminderId, occurrenceAt.atZone(zone).toInstant().toEpochMilli())
        val r = dao.getById(reminderId)
        if (r != null && r.status == ReminderStatus.COMPLETED.name) {
            dao.updateStatus(reminderId, ReminderStatus.ACTIVE.name)
        }
    }

    suspend fun completionSet(): Set<Pair<Long, Long>> =
        dao.completionsBetween(0, Long.MAX_VALUE).map { it.reminderId to it.occurrenceAt }.toSet()

    /** Expand every active reminder into concrete firings for [date]. */
    suspend fun occurrencesOn(date: LocalDate): List<Pair<Reminder, LocalDateTime>> {
        val completions = completionSet()
        val out = ArrayList<Pair<Reminder, LocalDateTime>>()
        for (entity in dao.getActive()) {
            val reminder = entity.toDomain(dao.alertsFor(entity.id).map { it.toSpec() })
            val start = reminder.startAt
            val dayOccurrences = RecurrenceEngine.occurrencesOn(reminder.rule, start, date)
                .ifEmpty {
                    if (start.toLocalDate() == date) listOf(start) else emptyList()
                }
            for (occ in dayOccurrences) {
                val key = reminder.id to occ.atZone(zone).toInstant().toEpochMilli()
                if (key !in completions) out += reminder to occ
            }
        }
        return out.sortedBy { it.second }
    }

    /** Next upcoming (not yet completed) occurrence across the whole app. */
    suspend fun nextOccurrence(now: LocalDateTime = LocalDateTime.now()): Occurrence? {
        val completions = completionSet()
        var best: Occurrence? = null
        for (entity in dao.getActive()) {
            val reminder = entity.toDomain(dao.alertsFor(entity.id).map { it.toSpec() })
            val next = RecurrenceEngine.nextOccurrence(reminder.rule, reminder.startAt, now)
                ?.takeIf { reminder.id to it.atZone(zone).toInstant().toEpochMilli() !in completions }
                ?: continue
            if (best == null || next < best.at) best = Occurrence(reminder.id, next)
        }
        return best
    }

    /** Today's pending + completed counts for the progress card (PRD §18). */
    suspend fun todayProgress(date: LocalDate = LocalDate.now()): Pair<Int, Int> {
        val all = occurrencesOn(date)
        val total = all.size
        // occurrencesOn already filters completed; recompute total including done:
        val completions = completionSet()
        var done = 0
        var grand = 0
        for (entity in dao.getActive()) {
            val reminder = entity.toDomain(emptyList())
            val occs = RecurrenceEngine.occurrencesOn(reminder.rule, reminder.startAt, date)
                .ifEmpty { if (reminder.startAt.toLocalDate() == date) listOf(reminder.startAt) else emptyList() }
            grand += occs.size
            for (occ in occs) {
                val key = reminder.id to occ.atZone(zone).toInstant().toEpochMilli()
                if (key in completions) done++
            }
        }
        // Also count non-recurring completions whose reminder flipped status
        return done to grand.coerceAtLeast(total)
    }

    // ---- mappers ------------------------------------------------------

    private fun ReminderEntity.toDomain(alerts: List<AlertSpec>) = Reminder(
        id = id,
        title = title,
        description = description,
        category = category,
        priority = runCatching { Priority.valueOf(priority) }.getOrDefault(Priority.NORMAL),
        startAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(startAt), zone),
        endAt = endAt?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zone) },
        timezone = timezone,
        rule = RepeatRule.parse(recurrence),
        status = runCatching { ReminderStatus.valueOf(status) }.getOrDefault(ReminderStatus.ACTIVE),
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        snoozeEnabled = snoozeEnabled,
        snoozeMinutes = snoozeMinutes,
        alerts = alerts.ifEmpty { listOf(AlertSpec()) },
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    private fun Reminder.toEntity(now: Long) = ReminderEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        priority = priority.name,
        startAt = startAt.atZone(zone).toInstant().toEpochMilli(),
        endAt = endAt?.atAt(zone),
        timezone = zone.id,
        recurrence = rule.serialize(),
        status = status.name,
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        snoozeEnabled = snoozeEnabled,
        snoozeMinutes = snoozeMinutes,
        createdAt = createdAt.takeIf { it != 0L } ?: now,
        updatedAt = now,
        deletedAt = deletedAt,
        syncVersion = 1,
        dirty = true,
    )

    private fun LocalDateTime.atAt(zone: ZoneId): Long = atZone(zone).toInstant().toEpochMilli()

    private fun ReminderAlertEntity.toSpec() = AlertSpec(offsetMinutes, enabled)
}
