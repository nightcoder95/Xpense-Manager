package com.example.core

import com.example.core.datetime.MonthRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthRangeTest {
    @Test fun feb2024_has29Days() = assertEquals(29, MonthRange.daysInMonth(2024, 2))
    @Test fun feb2026_has28Days() = assertEquals(28, MonthRange.daysInMonth(2026, 2))
    @Test fun june2026_has30Days() = assertEquals(30, MonthRange.daysInMonth(2026, 6))
    @Test fun rangeCoversWholeMonth() {
        val (s, e) = MonthRange.bounds(2026, 6)
        assertTrue(s < e)
    }
}
