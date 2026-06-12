package com.example.core

import com.example.core.common.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {
    private val f = CurrencyFormatter(symbol = "₹")
    @Test fun integerNoDecimals() = assertEquals("₹12,000", f.format(12000.0))
    @Test fun keepsDecimalsWhenPresent() = assertEquals("₹953.5", f.format(953.5))
}
