package com.example.ui

import com.example.ui.util.DayGrouping
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DayGroupingTest {
    private fun day(y: Int, m: Int, d: Int): Long = Calendar.getInstance().apply {
        clear(); set(y, m - 1, d, 12, 0)
    }.timeInMillis

    @Test fun pastMonthDate_isNotLabelledToday() {
        val now = day(2026, 6, 11)
        assertEquals(false, DayGrouping.label(day(2026, 1, 11), now) == "Today")
    }

    @Test fun todayLabel() {
        val now = day(2026, 6, 11)
        assertEquals("Today", DayGrouping.label(day(2026, 6, 11), now))
    }
}
