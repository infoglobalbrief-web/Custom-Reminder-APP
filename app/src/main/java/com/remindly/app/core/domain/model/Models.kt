package com.remindly.app.core.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** Priority levels (PRD §23 ORGANIZE). */
enum class Priority { LOW, NORMAL, HIGH }

/** Reminder lifecycle status (PRD §37). */
enum class ReminderStatus { ACTIVE, COMPLETED, ARCHIVED }

/** Recurrence frequencies (PRD §37 Recurrence). */
enum class Freq { NONE, DAILY, WEEKDAYS, WEEKLY, MONTHLY, YEARLY, CUSTOM }

/** Custom interval unit for Freq.CUSTOM. */
enum class IntervalUnit { DAYS, WEEKS, MONTHS }

/**
 * Central "Reminder Rule" (PRD §77) — one engine powers tasks,
 * birthdays, anniversaries and events.
 *
 * Serialized form (stable, compact, Room-friendly):
 *   NONE
 *   DAILY
 *   WEEKDAYS
 *   WEEKLY;MO,WE,FR
 *   MONTHLY
 *   YEARLY
 *   CUSTOM;3;WEEKS
 *   ...optional suffix ;UNTIL=yyyy-MM-dd
 */
data class RepeatRule(
    val freq: Freq = Freq.NONE,
    val interval: Int = 1,
    val unit: IntervalUnit = IntervalUnit.DAYS,
    val byDays: Set<DayOfWeek> = emptySet(),
    val until: LocalDate? = null,
) {
    fun serialize(): String {
        val base = when (freq) {
            Freq.NONE -> "NONE"
            Freq.DAILY -> "DAILY"
            Freq.WEEKDAYS -> "WEEKDAYS"
            Freq.WEEKLY -> "WEEKLY" + if (byDays.isNotEmpty()) {
                ";" + byDays.sorted().joinToString(",") { it.short }
            } else ""
            Freq.MONTHLY -> "MONTHLY"
            Freq.YEARLY -> "YEARLY"
            Freq.CUSTOM -> "CUSTOM;$interval;${unit.name}"
        }
        return if (until != null) "$base;UNTIL=$until" else base
    }

    companion object {
        val NONE = RepeatRule()

        fun parse(raw: String?): RepeatRule {
            if (raw.isNullOrBlank()) return NONE
            val parts = raw.split(";")
            val until = parts.firstOrNull { it.startsWith("UNTIL=") }
                ?.removePrefix("UNTIL=")
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val head = parts.filter { !it.startsWith("UNTIL=") }
            val freq = head.getOrNull(0)?.let { name ->
                runCatching { Freq.valueOf(name) }.getOrNull()
            } ?: Freq.NONE
            return when (freq) {
                Freq.WEEKLY -> {
                    val days = head.getOrNull(1)
                        ?.split(",")
                        ?.mapNotNull { short ->
                            DayOfWeek.entries.firstOrNull { it.short == short.uppercase(Locale.ROOT) }
                        }
                        ?.toSet()
                        ?: emptySet()
                    RepeatRule(freq = Freq.WEEKLY, byDays = days, until = until)
                }
                Freq.CUSTOM -> RepeatRule(
                    freq = Freq.CUSTOM,
                    interval = head.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    unit = head.getOrNull(2)?.let {
                        runCatching { IntervalUnit.valueOf(it) }.getOrNull()
                    } ?: IntervalUnit.DAYS,
                    until = until,
                )
                else -> RepeatRule(freq = freq, until = until)
            }
        }
    }
}

val DayOfWeek.short: String get() = getDisplayName(TextStyle.SHORT, Locale.ROOT).uppercase(Locale.ROOT)

/** Relative alert attached to a reminder (PRD §24 multiple alerts). */
data class AlertSpec(
    val offsetMinutes: Int = 0, // 0 = at time; >0 = minutes before event start
    val enabled: Boolean = true,
)

/** Full domain view of a reminder. */
data class Reminder(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val category: String = "Personal",
    val priority: Priority = Priority.NORMAL,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime? = null,
    val timezone: String,
    val rule: RepeatRule = RepeatRule.NONE,
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val alerts: List<AlertSpec> = listOf(AlertSpec()),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
) {
    /** True when the rule repeats forever or past a single fire. */
    val isRecurring: Boolean get() = rule.freq != Freq.NONE
}

/** A single computed firing time. */
data class Occurrence(
    val reminderId: Long,
    val at: LocalDateTime,
    val isBirthday: Boolean = false,
)

/** Person + birthday profile (PRD §25–26, §51–52). */
data class Person(
    val id: Long = 0L,
    val name: String,
    val relationship: String = "",
    val photoUri: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    val birthDate: LocalDate? = null,
    val birthdayEnabled: Boolean = true,
    val reminderStartDays: Int = 5,
    val reminderTime: LocalTime = LocalTime.of(22, 0),
    val birthdayDayAlert: Boolean = true,
    val createdAt: Long = 0L,
) {
    fun daysUntilBirthday(today: LocalDate = LocalDate.now()): Int? {
        val bd = birthDate ?: return null
        var next = LocalDate.of(today.year, bd.month, bd.dayOfMonth)
        if (next.isBefore(today)) next = LocalDate.of(today.year + 1, bd.month, bd.dayOfMonth)
        return java.time.temporal.ChronoUnit.DAYS.between(today, next).toInt()
    }

    fun nextBirthday(today: LocalDate = LocalDate.now()): LocalDate? {
        val bd = birthDate ?: return null
        var next = LocalDate.of(today.year, bd.month, bd.dayOfMonth)
        if (next.isBefore(today)) next = LocalDate.of(today.year + 1, bd.month, bd.dayOfMonth)
        return next
    }
}
