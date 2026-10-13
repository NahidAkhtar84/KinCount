package com.nahid.kincount.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.TimeZone

class DatesTest {

    private val originalTz = TimeZone.getDefault()
    private val originalLocale = Locale.getDefault()

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Dhaka"))
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTz)
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `day range covers exactly 24 hours starting at midnight`() {
        val (from, to) = Dates.dayRangeMillis(LocalDate.of(2026, 1, 10))
        assertEquals(86_400_000L, to - from)
        assertTrue(from < to)
    }

    @Test
    fun `timestamp round-trips through local date and time`() {
        val date = LocalDate.of(2026, 10, 13)
        val time = LocalTime.of(14, 30)
        val ts = Dates.timestampOf(date, time)
        assertEquals(date, Dates.localDateOf(ts))
        assertEquals(time, Dates.localTimeOf(ts))
    }

    @Test
    fun `day header is abbreviated weekday comma month day`() {
        assertEquals("Sat, Jan 10", Dates.formatDayHeader(LocalDate.of(2026, 1, 10)))
        assertEquals("Tue, Oct 13", Dates.formatDayHeader(LocalDate.of(2026, 10, 13)))
    }

    @Test
    fun `time format is 12 hour lowercase`() {
        val ts = Dates.timestampOf(LocalDate.of(2026, 1, 10), LocalTime.of(13, 30))
        assertEquals("1:30 pm", Dates.formatTime(ts))
        val morning = Dates.timestampOf(LocalDate.of(2026, 1, 10), LocalTime.of(9, 5))
        assertEquals("9:05 am", Dates.formatTime(morning))
    }
}
