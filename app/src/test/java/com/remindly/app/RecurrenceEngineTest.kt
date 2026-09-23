package com.remindly.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.remindly.app.core.domain.model.Freq
import com.remindly.app.core.domain.model.IntervalUnit
import com.remindly.app.core.domain.model.Person
import com.remindly.app.core.domain.model.RepeatRule
import com.remindly.app.core.domain.model.short
import com.remindly.app.core.domain.recurrence.RecurrenceEngine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * PRD §71 testing strategy + §72 MANDATED QA test.
 * Time is the core product — these tests pin the recurrence engine.
 */
class RecurrenceEngineTest {

    // ------------------------------------------------------------------
    // PRD §72 — exact example from the document (Father's Birthday)
    //   Person: Father · Birthday: 30 October 2026
    //   Start: 25 October 2026 · Time: 22:00 · Frequency: Daily
    //   Expected: 25,26,27,28,29,30 Oct @ 22:00 ✓ · 31 Oct → none ✓
    //   Next cycle: 30 Oct 2027 (yearly rule regenerates) ✓
    // ------------------------------------------------------------------
    @Test
    fun qa_fatherBirthdayDailyWindow_mandatoryExample() {
        val birthday = LocalDate.of(2026, 10, 30)
        val person = Person(
            name = "Father",
            relationship = "Father",
            birthDate = birthday,
            reminderStartDays = 5,
            reminderTime = LocalTime.of(22, 0),
            birthdayDayAlert = true,
        )

        val window = RecurrenceEngine.birthdayAlertWindow(
            birthday = birthday,
            startDays = 5,
            time = LocalTime.of(22, 0),
            from = LocalDate.of(2026, 10, 1),
            to = LocalDate.of(2026, 12, 31),
        )

        val expected = listOf(25, 26, 27, 28, 29, 30).map { day ->
            LocalDateTime.of(2026, 10, day, 22, 0)
        }
        assertEquals(expected, window)

        // 31 October — NO notification
        assertFalse(window.any { it.dayOfMonth == 31 })

        // Next cycle: the yearly birthday rule generates next year's window
        val nextYearWindow = RecurrenceEngine.birthdayAlertWindow(
            birthday = LocalDate.of(2027, 10, 30),
            startDays = 5,
            time = LocalTime.of(22, 0),
        )
        assertTrue(nextYearWindow.contains(LocalDateTime.of(2027, 10, 30, 22, 0)))
        assertTrue(nextYearWindow.contains(LocalDateTime.of(2027, 10, 25, 22, 0)))

        // nextBirthdayAlert from after this year's window rolls into next cycle
        val afterWindow = LocalDateTime.of(2026, 10, 31, 0, 0)
        val next = RecurrenceEngine.nextBirthdayAlert(person, afterWindow)
        assertNotNull(next)
        assertEquals(LocalDate.of(2027, 10, 25), next!!.toLocalDate()) // window reopens at start
        assertEquals(LocalTime.of(22, 0), next.toLocalTime())
    }

    @Test
    fun birthday_nextBirthdayDate_afterBirthday_rollsToNextYear() {
        val person = Person(name = "Dad", birthDate = LocalDate.of(1980, 10, 30))
        val next = person.nextBirthday(LocalDate.of(2026, 11, 1))
        assertEquals(LocalDate.of(2027, 10, 30), next)
        assertEquals(334, person.daysUntilBirthday(LocalDate.of(2026, 11, 1))) // sanity on day math
    }

    // ------------------------------------------------------------------
    // PRD §71 — Recurrence
    // ------------------------------------------------------------------
    @Test
    fun daily_everyDayAtSameTime() {
        val start = LocalDateTime.of(2026, 9, 23, 8, 0)
        val rule = RepeatRule(Freq.DAILY)
        assertEquals(start, RecurrenceEngine.nextOccurrence(rule, start, start))
        assertEquals(
            LocalDateTime.of(2026, 9, 24, 8, 0),
            RecurrenceEngine.nextOccurrence(rule, start, start.plusNanos(1)),
        )
        val week = RecurrenceEngine.occurrencesBetween(rule, start, start, start.plusDays(7))
        assertEquals(7, week.size) // from start inclusive through day 6 → 7 firings in [start, start+7d)
    }

    @Test
    fun weekdays_skipsWeekend() {
        // 2026-09-23 is a Wednesday
        val start = LocalDateTime.of(2026, 9, 23, 9, 0)
        val rule = RepeatRule(Freq.WEEKDAYS)
        val next = RecurrenceEngine.nextOccurrence(rule, start, LocalDateTime.of(2026, 9, 25, 10, 0))
        // Sat 26 / Sun 27 skipped → Monday 28
        assertEquals(LocalDate.of(2026, 9, 28), next?.toLocalDate())
    }

    @Test
    fun weekly_respectsByDay() {
        // Next Monday from Wed 23 Sep 2026
        val start = LocalDateTime.of(2026, 9, 23, 18, 0)
        val rule = RepeatRule(Freq.WEEKLY, byDays = setOf(DayOfWeek.MONDAY))
        val next = RecurrenceEngine.nextOccurrence(rule, start, LocalDateTime.of(2026, 9, 24, 0, 0))
        assertEquals(LocalDate.of(2026, 9, 28), next?.toLocalDate()) // Monday
        assertEquals(DayOfWeek.MONDAY, next?.dayOfWeek)
    }

    @Test
    fun monthly_sameDayOfMonth_handlesShortMonths() {
        // Start Jan 31 → next occurrences skip invalid days safely
        val start = LocalDateTime.of(2026, 1, 31, 10, 0)
        val rule = RepeatRule(Freq.MONTHLY)
        val next = RecurrenceEngine.nextOccurrence(rule, start, LocalDateTime.of(2026, 2, 1, 0, 0))
        // February has no 31st → clamped to 28
        assertEquals(LocalDate.of(2026, 2, 28), next?.toLocalDate())
    }

    @Test
    fun yearly_leapYearFeb29() {
        val start = LocalDateTime.of(2024, 2, 29, 12, 0) // leap year anchor
        val rule = RepeatRule(Freq.YEARLY)
        // From mid-winter 2025 the next yearly fire is 28 Feb (2025 not leap)
        val next = RecurrenceEngine.nextOccurrence(rule, start, LocalDateTime.of(2025, 1, 1, 0, 0))
        assertEquals(LocalDate.of(2025, 2, 28), next?.toLocalDate())
        // After that date has passed, the engine rolls to the following year
        val after = RecurrenceEngine.nextOccurrence(rule, start, LocalDateTime.of(2025, 3, 1, 0, 0))
        assertEquals(LocalDate.of(2026, 2, 28), after?.toLocalDate())
    }

    @Test
    fun until_endDateStopsGeneration() {
        val start = LocalDateTime.of(2026, 10, 25, 22, 0)
        val rule = RepeatRule(Freq.DAILY, until = LocalDate.of(2026, 10, 30))
        val all = RecurrenceEngine.occurrencesBetween(
            rule, start,
            LocalDateTime.of(2026, 10, 1, 0, 0),
            LocalDateTime.of(2026, 11, 15, 0, 0),
        )
        assertEquals(listOf(25, 26, 27, 28, 29, 30), all.map { it.dayOfMonth })
        assertNull(RecurrenceEngine.nextOccurrence(rule, start, LocalDateTime.of(2026, 10, 31, 0, 0)))
    }

    @Test
    fun custom_interval_every3Days() {
        val start = LocalDateTime.of(2026, 9, 1, 7, 30)
        val rule = RepeatRule(Freq.CUSTOM, interval = 3, unit = IntervalUnit.DAYS)
        val next = RecurrenceEngine.nextOccurrence(rule, start, LocalDateTime.of(2026, 9, 2, 0, 0))
        assertEquals(LocalDate.of(2026, 9, 4), next?.toLocalDate())
        val next2 = RecurrenceEngine.nextOccurrence(rule, start, next!!.plusNanos(1))
        assertEquals(LocalDate.of(2026, 9, 7), next2?.toLocalDate())
    }

    @Test
    fun once_noneFiresOnlyAtStart() {
        val start = LocalDateTime.of(2026, 9, 23, 20, 0)
        val rule = RepeatRule.NONE
        assertEquals(start, RecurrenceEngine.nextOccurrence(rule, start, start))
        assertNull(RecurrenceEngine.nextOccurrence(rule, start, start.plusNanos(1)))
    }

    // ------------------------------------------------------------------
    // PRD §71 — Time edge cases
    // ------------------------------------------------------------------
    @Test
    fun timeEdge_2359_crossesMidnight() {
        val start = LocalDateTime.of(2026, 9, 23, 23, 59)
        val rule = RepeatRule(Freq.DAILY)
        val next = RecurrenceEngine.nextOccurrence(rule, start, LocalDateTime.of(2026, 9, 24, 0, 0))
        assertEquals(LocalDateTime.of(2026, 9, 24, 23, 59), next)
    }

    @Test
    fun timeEdge_monthAndYearChange() {
        val start = LocalDateTime.of(2026, 12, 31, 23, 0)
        val rule = RepeatRule(Freq.DAILY)
        val next = RecurrenceEngine.nextOccurrence(rule, start, LocalDateTime.of(2027, 1, 1, 0, 0))
        assertEquals(LocalDate.of(2027, 1, 1), next?.toLocalDate())
    }

    // ------------------------------------------------------------------
    // Rule serialization round-trip (storage layer)
    // ------------------------------------------------------------------
    @Test
    fun rule_serialization_roundTrip() {
        val rules = listOf(
            RepeatRule.NONE,
            RepeatRule(Freq.DAILY),
            RepeatRule(Freq.WEEKDAYS),
            RepeatRule(Freq.WEEKLY, byDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
            RepeatRule(Freq.MONTHLY),
            RepeatRule(Freq.YEARLY),
            RepeatRule(Freq.CUSTOM, interval = 2, unit = IntervalUnit.WEEKS),
            RepeatRule(Freq.DAILY, until = LocalDate.of(2026, 12, 31)),
            RepeatRule(Freq.WEEKLY, byDays = setOf(DayOfWeek.SUNDAY), until = LocalDate.of(2027, 1, 1)),
        )
        for (rule in rules) {
            val parsed = RepeatRule.parse(rule.serialize())
            assertEquals(rule.freq, parsed.freq)
            assertEquals(rule.interval, parsed.interval)
            assertEquals(rule.unit, parsed.unit)
            assertEquals(rule.byDays, parsed.byDays)
            assertEquals(rule.until, parsed.until)
        }
    }

    @Test
    fun dayPart_buckets() {
        assertEquals(RecurrenceEngine.DayPart.MORNING, RecurrenceEngine.dayPart(LocalTime.of(7, 0)))
        assertEquals(RecurrenceEngine.DayPart.AFTERNOON, RecurrenceEngine.dayPart(LocalTime.of(13, 0)))
        assertEquals(RecurrenceEngine.DayPart.EVENING, RecurrenceEngine.dayPart(LocalTime.of(20, 0)))
    }

    @Test
    fun dayOfWeek_shortCodes_stable() {
        assertEquals("MO", DayOfWeek.MONDAY.short)
        assertEquals("SU", DayOfWeek.SUNDAY.short)
    }
}
