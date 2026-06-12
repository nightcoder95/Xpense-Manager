package com.example.ui.util

import java.util.Calendar

object DayGrouping {
    // Returns "Today"/"Yesterday" only when the txn date is actually today/yesterday relative to `now`.
    fun label(date: Long, now: Long = System.currentTimeMillis()): String {
        val d = Calendar.getInstance().apply { timeInMillis = date }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val yest = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DATE, -1) }
        fun sameDay(a: Calendar, b: Calendar) =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        return when {
            sameDay(d, today) -> "Today"
            sameDay(d, yest) -> "Yesterday"
            else -> DateTimeUtils.formatDate(date)
        }
    }
}
