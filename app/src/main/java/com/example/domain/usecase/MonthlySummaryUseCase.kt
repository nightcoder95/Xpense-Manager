package com.example.domain.usecase

import com.example.domain.model.MonthlySummary
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType

class MonthlySummaryUseCase {
    fun from(txns: List<Transaction>) = MonthlySummary(
        income = txns.filter { it.type == TxnType.INCOME }.sumOf { it.amount },
        spending = txns.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }
    )
}
