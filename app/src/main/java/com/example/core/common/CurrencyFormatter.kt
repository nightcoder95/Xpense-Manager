package com.example.core.common

import java.util.Locale

class CurrencyFormatter(
    private val symbol: String = "₹",
    private val locale: Locale = Locale.US
) {
    fun format(amount: Double): String {
        val s = if (amount % 1.0 == 0.0) {
            String.format(locale, "%,d", amount.toLong())
        } else {
            String.format(locale, "%,.1f", amount)
        }
        return "$symbol$s"
    }
}
