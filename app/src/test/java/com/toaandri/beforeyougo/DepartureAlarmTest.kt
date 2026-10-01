package com.toaandri.beforeyougo

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class DepartureAlarmTest {
    private fun now(value: String) = ZonedDateTime.parse(value)

    @Test fun weekdaysSkipWeekendAndRollToNextWeek() {
        val alarm = DepartureAlarm(1)
        assertEquals("2026-10-05T08:00+03:00[Europe/Istanbul]",
            nextOccurrence(alarm, now("2026-10-02T08:01+03:00[Europe/Istanbul]")).toString())
    }

    @Test fun selectedDaysAndDifferentTimesRemainIndependent() {
        val current = now("2026-10-05T07:00+03:00[Europe/Istanbul]")
        assertEquals(5, nextOccurrence(DepartureAlarm(1, hour = 7, minute = 30, days = setOf(1)), current)!!.dayOfMonth)
        assertEquals(6, nextOccurrence(DepartureAlarm(2, hour = 10, days = setOf(2)), current)!!.dayOfMonth)
        assertNull(nextOccurrence(DepartureAlarm(3, enabled = false), current))
        assertNull(nextOccurrence(DepartureAlarm(4, days = emptySet()), current))
    }

    @Test fun anOccurrenceAtOrBeforeNowNeverFiresAgain() {
        val current = now("2026-10-05T08:00+03:00[Europe/Istanbul]")
        assertEquals(12, nextOccurrence(DepartureAlarm(1, days = setOf(1)), current)!!.dayOfMonth)
    }

    @Test fun skippingOneOccurrencePreservesLaterRepetitions() {
        val current = now("2026-10-05T07:00+03:00[Europe/Istanbul]")
        val alarm = DepartureAlarm(1, days = setOf(1))
        val skipped = alarm.copy(skipUntil = nextOccurrence(alarm, current)!!.toInstant().toEpochMilli())
        assertEquals(12, nextOccurrence(skipped, current)!!.dayOfMonth)
        assertEquals(5, nextOccurrence(skipped.copy(skipUntil = 0), current)!!.dayOfMonth)
    }

    @Test fun springDstGapMovesNonexistentTimeForward() {
        val alarm = DepartureAlarm(1, hour = 2, minute = 30, days = setOf(7))
        val next = nextOccurrence(alarm, now("2026-03-29T01:00+01:00[Europe/Paris]"))!!
        assertEquals(3, next.hour)
        assertEquals(30, next.minute)
    }

    @Test fun fallDstOverlapDoesNotRingTwice() {
        val alarm = DepartureAlarm(1, hour = 2, minute = 30, days = setOf(7))
        // During the second 02:15 the first occurrence has already passed.
        val next = nextOccurrence(alarm, now("2026-10-25T02:15+01:00[Europe/Paris]"))!!
        assertEquals(LocalDate.of(2026, 11, 1), next.toLocalDate())
        assertEquals(2, next.hour)
    }

    @Test fun customChecklistRespectsDateArchiveAndMembership() {
        val objects = listOf(SavedItem(1, "Clés", true, null), SavedItem(2, "Gourde", true, null),
            SavedItem(3, "Billet", false, "2026-10-10"), SavedItem(4, "Ancien", true, null, archived = true))
        val alarm = DepartureAlarm(1, allItems = false, itemIds = setOf(1, 3, 4))
        assertEquals(listOf(1L), itemsForAlarm(objects, alarm, LocalDate.of(2026, 10, 2)).map { it.id })
        assertEquals(listOf(1L, 2L), itemsForAlarm(objects, alarm.copy(allItems = true), LocalDate.of(2026, 10, 2)).map { it.id })
    }
}
