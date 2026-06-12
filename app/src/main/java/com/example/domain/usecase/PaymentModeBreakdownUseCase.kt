package com.example.domain.usecase

import com.example.domain.model.Account
import com.example.domain.model.CategorySlice
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType

/** Groups transactions of one [TxnType] by source account, mirroring [CategoryBreakdownUseCase]. */
class PaymentModeBreakdownUseCase {
    fun from(txns: List<Transaction>, accounts: List<Account>, type: TxnType): List<CategorySlice> {
        val byId = accounts.associateBy { it.id }
        val filtered = txns.filter { it.type == type }
        val total = filtered.sumOf { it.amount }
        return filtered.groupBy { it.accountId }
            .map { (accountId, list) ->
                val acc = byId[accountId]
                val amt = list.sumOf { it.amount }
                CategorySlice(
                    category = acc?.name ?: "Unknown",
                    amount = amt,
                    colorHex = acc?.colorHex ?: "#6C5DD3",
                    percent = if (total > 0) amt / total * 100 else 0.0
                )
            }
            .sortedByDescending { it.amount }
    }
}
