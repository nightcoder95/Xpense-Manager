package com.example.domain.usecase

import com.example.domain.model.Transaction

class SearchTransactionsUseCase {
    fun filter(
        txns: List<Transaction>,
        query: String,
        categories: Set<String>,
        accountIds: Set<Long>
    ): List<Transaction> = txns.filter { t ->
        val q = query.isBlank() || listOf(t.note, t.category, t.tag).any { it.contains(query, true) }
        val c = categories.isEmpty() || t.category in categories
        val a = accountIds.isEmpty() || t.accountId in accountIds
        q && c && a
    }
}
