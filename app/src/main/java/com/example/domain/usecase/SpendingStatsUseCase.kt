package com.example.domain.usecase

import com.example.core.datetime.MonthRange
import com.example.domain.model.SpendingStats
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType

class SpendingStatsUseCase {
    fun from(txns: List<Transaction>, year: Int, month: Int): SpendingStats {
        val exp = txns.filter { it.type == TxnType.EXPENSE }
        val inc = txns.filter { it.type == TxnType.INCOME }
        val spend = exp.sumOf { it.amount }
        val days = MonthRange.daysInMonth(year, month)
        return SpendingStats(
            avgPerDay = if (days > 0) spend / days else 0.0,
            avgPerTxn = if (exp.isNotEmpty()) spend / exp.size else 0.0,
            txnCount = exp.size,
            avgIncomePerTxn = if (inc.isNotEmpty()) inc.sumOf { it.amount } / inc.size else 0.0
        )
    }
}
