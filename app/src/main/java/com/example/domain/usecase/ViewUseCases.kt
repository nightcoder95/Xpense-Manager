package com.example.domain.usecase

import com.example.domain.model.MonthlySummary
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType
import java.time.Instant
import java.time.ZoneId

/** Transactions for a single day plus that day's summary. */
data class DayView(val transactions: List<Transaction>, val summary: MonthlySummary)

class GetDayViewUseCase {
    private val summary = MonthlySummaryUseCase()
    fun from(txns: List<Transaction>, dayStart: Long, dayEnd: Long): DayView {
        val ofDay = txns.filter { it.date in dayStart..dayEnd }.sortedByDescending { it.date }
        return DayView(ofDay, summary.from(ofDay))
    }
}

/** Per-day income/spending totals for a calendar grid. */
data class CalendarDay(val day: Int, val income: Double, val spending: Double) {
    val net: Double get() = income - spending
}

class GetCalendarMonthUseCase {
    fun from(txns: List<Transaction>, monthStart: Long, monthEnd: Long, zone: ZoneId = ZoneId.systemDefault()): Map<Int, CalendarDay> {
        return txns.filter { it.date in monthStart..monthEnd }
            .groupBy { Instant.ofEpochMilli(it.date).atZone(zone).dayOfMonth }
            .mapValues { (day, list) ->
                CalendarDay(
                    day = day,
                    income = list.filter { it.type == TxnType.INCOME }.sumOf { it.amount },
                    spending = list.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }
                )
            }
    }
}
