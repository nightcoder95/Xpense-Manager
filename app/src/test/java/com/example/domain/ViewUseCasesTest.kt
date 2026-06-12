package com.example.domain

import com.example.domain.model.Transaction
import com.example.domain.model.TxnType
import com.example.domain.usecase.GetCalendarMonthUseCase
import com.example.domain.usecase.GetDayViewUseCase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ViewUseCasesTest {
    private val zone: ZoneId = ZoneId.systemDefault()
    private fun millis(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun `day view returns only that day's txns with summary`() {
        val txns = listOf(
            Transaction(1, TxnType.EXPENSE, 100.0, "x", 1, null, millis(2026, 6, 11), ""),
            Transaction(2, TxnType.INCOME, 500.0, "y", 1, null, millis(2026, 6, 11), ""),
            Transaction(3, TxnType.EXPENSE, 9.0, "z", 1, null, millis(2026, 6, 12), "")
        )
        val dayStart = LocalDate.of(2026, 6, 11).atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = LocalDate.of(2026, 6, 11).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        val v = GetDayViewUseCase().from(txns, dayStart, dayEnd)
        assertEquals(2, v.transactions.size)
        assertEquals(100.0, v.summary.spending, 0.0)
        assertEquals(500.0, v.summary.income, 0.0)
    }

    @Test fun `calendar groups income and spending per day`() {
        val txns = listOf(
            Transaction(1, TxnType.EXPENSE, 100.0, "x", 1, null, millis(2026, 6, 11), ""),
            Transaction(2, TxnType.INCOME, 500.0, "y", 1, null, millis(2026, 6, 11), ""),
            Transaction(3, TxnType.EXPENSE, 9.0, "z", 1, null, millis(2026, 6, 12), "")
        )
        val (s, e) = com.example.core.datetime.MonthRange.bounds(2026, 6)
        val map = GetCalendarMonthUseCase().from(txns, s, e)
        assertEquals(100.0, map[11]!!.spending, 0.0)
        assertEquals(500.0, map[11]!!.income, 0.0)
        assertEquals(9.0, map[12]!!.spending, 0.0)
    }
}
