package com.nahid.kincount.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Dates {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val dayHeaderFormat = DateTimeFormatter.ofPattern("EEE, MMM d")
    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

    fun dayRangeMillis(day: LocalDate): Pair<Long, Long> {
        val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return from to to
    }

    fun timestampOf(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    fun localDateOf(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    fun localTimeOf(timestamp: Long): LocalTime =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime()

    fun formatDayHeader(day: LocalDate): String = day.format(dayHeaderFormat)

    fun formatTime(timestamp: Long): String =
        localTimeOf(timestamp).format(timeFormat).lowercase()
}
