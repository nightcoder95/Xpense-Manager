package com.example.core.datetime

import java.time.YearMonth
import java.time.ZoneId

object MonthRange {
    fun daysInMonth(year: Int, month: Int) = YearMonth.of(year, month).lengthOfMonth()

    fun bounds(year: Int, month: Int): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val ym = YearMonth.of(year, month)
        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }
}

/** Start/end epoch-millis bounds for the period kinds the Analysis screen offers. */
object RangeBounds {
    private val zone = ZoneId.systemDefault()
    private fun startOf(d: java.time.LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun endOf(d: java.time.LocalDate) =
        d.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()

    fun day(anchor: java.time.LocalDate): Pair<Long, Long> = startOf(anchor) to endOf(anchor)

    fun week(anchor: java.time.LocalDate): Pair<Long, Long> {
        val monday = anchor.with(java.time.DayOfWeek.MONDAY)
        return startOf(monday) to endOf(monday.plusDays(6))
    }

    fun month(anchor: java.time.LocalDate): Pair<Long, Long> =
        MonthRange.bounds(anchor.year, anchor.monthValue)

    fun year(anchor: java.time.LocalDate): Pair<Long, Long> =
        startOf(java.time.LocalDate.of(anchor.year, 1, 1)) to endOf(java.time.LocalDate.of(anchor.year, 12, 31))

    /** Inclusive day count between two epoch-millis bounds — for per-day averages (E#6). */
    fun dayCount(start: Long, end: Long): Int {
        val s = java.time.Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
        val e = java.time.Instant.ofEpochMilli(end).atZone(zone).toLocalDate()
        return (java.time.temporal.ChronoUnit.DAYS.between(s, e) + 1).toInt().coerceAtLeast(1)
    }
}
