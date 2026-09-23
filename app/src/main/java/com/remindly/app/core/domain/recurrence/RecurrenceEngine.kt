package com.remindly.app.core.domain.recurrence

import com.remindly.app.core.domain.model.Freq
import com.remindly.app.core.domain.model.IntervalUnit
import com.remindly.app.core.domain.model.Occurrence
import com.remindly.app.core.domain.model.Person
import com.remindly.app.core.domain.model.RepeatRule
import com.remindly.app.core.domain.model.short
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Occurrence calculator (PRD §44 Reminder Engine, §56 Schedule Strategy,
 * §72 mandatory QA test).
 *
 * Pure Kotlin / java.time — no Android dependencies — so it is fully
 * unit-testable and deterministic across timezones, month lengths,
 * leap years and DST (PRD §71).
 *
 * Reminder → Recurrence Rule → Occurrence Calculator → Next Occurrence
 *          → Local Scheduler → OS Notification
 */
object RecurrenceEngine {

    const val MAX_ITERATIONS = 3_660 // ~10 years of daily scans — hard safety cap

    /** First firing at or after [from], or null when the rule is exhausted. */
    fun nextOccurrence(
        rule: RepeatRule,
        start: LocalDateTime,
        from: LocalDateTime = start,
    ): LocalDateTime? {
        if (rule.freq == Freq.NONE) return if (!start.isBefore(from)) start else null
        val notBefore = if (from.isBefore(start)) start else from
        return when (rule.freq) {
            Freq.NONE -> null
            Freq.DAILY -> daily(rule, start, notBefore, 1)
            Freq.WEEKDAYS -> {
                var cursor = notBefore.toLocalDate()
                var guard = 0
                while (guard++ < MAX_ITERATIONS) {
                    if (cursor.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                        cursor = cursor.plusDays(1); continue
                    }
                    val candidate = at(cursor, start.toLocalTime())
                    if (!candidate.isBefore(notBefore) && !isAfterUntil(rule, candidate)) return candidate
                    cursor = cursor.plusDays(1)
                }
                null
            }
            Freq.WEEKLY -> weekly(rule, start, notBefore)
            Freq.MONTHLY -> monthly(rule, start, notBefore)
            Freq.YEARLY -> yearly(rule, start, notBefore)
            Freq.CUSTOM -> custom(rule, start, notBefore)
        }
    }

    /** All firing times in [from, to) for the given rule. */
    fun occurrencesBetween(
        rule: RepeatRule,
        start: LocalDateTime,
        from: LocalDateTime,
        to: LocalDateTime,
        limit: Int = 400,
    ): List<LocalDateTime> {
        val out = ArrayList<LocalDateTime>()
        var cursor = from
        var guard = 0
        while (out.size < limit && guard++ < MAX_ITERATIONS) {
            val next = nextOccurrence(rule, start, cursor) ?: break
            if (!next.isBefore(to)) break
            out += next
            cursor = next.plusNanos(1)
        }
        return out
    }

    /** Occurrences falling on [date] (used by Home / Tasks / Calendar). */
    fun occurrencesOn(
        rule: RepeatRule,
        start: LocalDateTime,
        date: LocalDate,
    ): List<LocalDateTime> {
        val dayStart = date.atStartOfDay()
        val dayEnd = date.plusDays(1).atStartOfDay()
        return occurrencesBetween(rule, start, dayStart, dayEnd)
    }

    // ------------------------------------------------------------------
    // Birthday engine (PRD §26, §44, §72)
    // ------------------------------------------------------------------

    /**
     * Alert window for a birthday: every day from
     * (birthday − reminderStartDays) through the birthday itself at
     * [time], inclusive — never the day after (QA: 31 Oct → none).
     *
     * Example (PRD §72): birthday 30 Oct 2026, start 25 Oct, 22:00,
     * daily → 25,26,27,28,29,30 Oct @ 22:00 and nothing on 31 Oct.
     */
    fun birthdayAlertWindow(
        birthday: LocalDate,
        startDays: Int,
        time: LocalTime,
        from: LocalDate = LocalDate.MIN,
        to: LocalDate = LocalDate.MAX,
    ): List<LocalDateTime> {
        val windowStart = birthday.minusDays(startDays.coerceAtLeast(0).toLong())
        val out = ArrayList<LocalDateTime>()
        var cursor = windowStart
        var guard = 0
        while (!cursor.isAfter(birthday) && guard++ < 400) {
            if (!cursor.isBefore(from) && !cursor.isAfter(to)) {
                out += cursor.atTime(time)
            }
            cursor = cursor.plusDays(1)
        }
        return out
    }

    /**
     * Next alert strictly after [from] for this person's birthday,
     * scanning this year and the next (PRD §72 "next cycle" — the yearly
     * rule regenerates the following year's window automatically).
     */
    fun nextBirthdayAlert(
        person: Person,
        from: LocalDateTime,
    ): LocalDateTime? {
        val bd = person.birthDate ?: return null
        val time = person.reminderTime
        val days = person.reminderStartDays
        for (year in from.year..(from.year + 2)) {
            val thisBd = try {
                LocalDate.of(year, bd.monthValue, bd.dayOfMonth)
            } catch (_: java.time.DateTimeException) {
                // Leap-day birthdays (29 Feb) fall back to 28 Feb in non-leap years.
                LocalDate.of(year, bd.monthValue, 28)
            }
            val window = birthdayAlertWindow(thisBd, days, time)
            window.firstOrNull { it.isAfter(from) }?.let { return it }
        }
        return null
    }

    /** Next yearly occurrence of the birthday date itself (countdown card). */
    fun nextBirthdayDate(person: Person, today: LocalDate = LocalDate.now()): LocalDate? =
        person.nextBirthday(today)

    // ------------------------------------------------------------------
    // Frequency helpers
    // ------------------------------------------------------------------

    private fun isAfterUntil(rule: RepeatRule, candidate: LocalDateTime): Boolean {
        val until = rule.until ?: return false
        return candidate.toLocalDate().isAfter(until)
    }

    private fun at(date: LocalDate, time: LocalTime): LocalDateTime = date.atTime(time)

    private fun daily(rule: RepeatRule, start: LocalDateTime, from: LocalDateTime, stepDays: Int): LocalDateTime? {
        val time = start.toLocalTime()
        val daysBetween = ChronoUnit.DAYS.between(start.toLocalDate(), from.toLocalDate())
        val interval = (rule.interval * stepDays).coerceAtLeast(1)
        var offset = if (daysBetween <= 0) 0L else ((daysBetween + interval - 1) / interval) * interval
        var candidateDate = start.toLocalDate().plusDays(offset)
        var candidate = at(candidateDate, time)
        // Align to interval grid from the original start date.
        while (candidate.isBefore(from)) {
            offset += interval
            candidateDate = start.toLocalDate().plusDays(offset)
            candidate = at(candidateDate, time)
        }
        if (isAfterUntil(rule, candidate)) return null
        return candidate
    }

    private fun weekly(rule: RepeatRule, start: LocalDateTime, from: LocalDateTime): LocalDateTime? {
        val days = if (rule.byDays.isEmpty()) setOf(start.dayOfWeek) else rule.byDays
        val time = start.toLocalTime()
        val interval = rule.interval.coerceAtLeast(1)
        val weeksBetween = ChronoUnit.WEEKS.between(start.toLocalDate().with(DayOfWeek.MONDAY), from.toLocalDate().with(DayOfWeek.MONDAY))
        var weekOffset = if (weeksBetween <= 0) 0L else ((weeksBetween + interval - 1) / interval) * interval
        var guard = 0
        while (guard++ < MAX_ITERATIONS) {
            val weekStart = start.toLocalDate().with(DayOfWeek.MONDAY).plusWeeks(weekOffset)
            for (dow in DayOfWeek.entries) {
                if (dow !in days) continue
                val date = weekStart.with(dow)
                val candidate = at(date, time)
                if (!candidate.isBefore(from) && !candidate.isBefore(start)) {
                    if (isAfterUntil(rule, candidate)) return null
                    return candidate
                }
            }
            weekOffset += interval
        }
        return null
    }

    private fun monthly(rule: RepeatRule, start: LocalDateTime, from: LocalDateTime): LocalDateTime? {
        val time = start.toLocalTime()
        val dom = start.dayOfMonth
        val interval = rule.interval.coerceAtLeast(1)
        var monthsBetween = ChronoUnit.MONTHS.between(
            start.toLocalDate().withDayOfMonth(1),
            from.toLocalDate().withDayOfMonth(1),
        )
        var monthOffset = if (monthsBetween <= 0) 0L else ((monthsBetween + interval - 1) / interval) * interval
        var guard = 0
        while (guard++ < MAX_ITERATIONS) {
            val candidateDate = safeDayOfMonth(start.toLocalDate().plusMonths(monthOffset), dom)
            val candidate = at(candidateDate, time)
            if (!candidate.isBefore(from) && !candidate.isBefore(start)) {
                if (isAfterUntil(rule, candidate)) return null
                return candidate
            }
            monthOffset += interval
        }
        return null
    }

    private fun yearly(rule: RepeatRule, start: LocalDateTime, from: LocalDateTime): LocalDateTime? {
        val time = start.toLocalTime()
        val md = java.time.MonthDay.from(start)
        var year = from.year
        var guard = 0
        while (guard++ < 80) {
            val candidateDate = try {
                LocalDate.of(year, md.monthValue, md.dayOfMonth)
            } catch (_: java.time.DateTimeException) {
                // 29 Feb → 28 Feb in non-leap years
                LocalDate.of(year, md.monthValue, 28)
            }
            val candidate = at(candidateDate, time)
            if (!candidate.isBefore(from) && !candidate.isBefore(start)) {
                if (isAfterUntil(rule, candidate)) return null
                return candidate
            }
            year++
        }
        return null
    }

    private fun custom(rule: RepeatRule, start: LocalDateTime, from: LocalDateTime): LocalDateTime? {
        val time = start.toLocalTime()
        val step = rule.interval.coerceAtLeast(1)
        return when (rule.unit) {
            IntervalUnit.DAYS -> daily(rule.copy(interval = 1), start, from, step)
            IntervalUnit.WEEKS -> {
                val weeksBetween = ChronoUnit.WEEKS.between(start.toLocalDate(), from.toLocalDate())
                var offset = if (weeksBetween <= 0) 0L else ((weeksBetween + step - 1) / step) * step
                var candidate = at(start.toLocalDate().plusWeeks(offset), time)
                while (candidate.isBefore(from)) {
                    offset += step
                    candidate = at(start.toLocalDate().plusWeeks(offset), time)
                }
                if (isAfterUntil(rule, candidate)) null else candidate
            }
            IntervalUnit.MONTHS -> monthly(rule, start, from)
        }
    }

    private fun safeDayOfMonth(base: LocalDate, day: Int): LocalDate {
        val length = base.lengthOfMonth()
        return base.withDayOfMonth(day.coerceAtMost(length))
    }

    // ------------------------------------------------------------------
    // Time-of-day bucketing for Tasks screen (PRD §30)
    // ------------------------------------------------------------------

    enum class DayPart { MORNING, AFTERNOON, EVENING }

    fun dayPart(time: LocalTime): DayPart = when {
        time.hour < 12 -> DayPart.MORNING
        time.hour < 17 -> DayPart.AFTERNOON
        else -> DayPart.EVENING
    }
}
