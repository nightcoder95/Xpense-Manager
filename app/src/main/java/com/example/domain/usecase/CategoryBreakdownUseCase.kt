package com.example.domain.usecase

import com.example.domain.model.Category
import com.example.domain.model.CategorySlice
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType

class CategoryBreakdownUseCase {
    fun from(txns: List<Transaction>, categories: List<Category>, type: TxnType): List<CategorySlice> {
        val filtered = txns.filter { it.type == type }
        val total = filtered.sumOf { it.amount }
        return filtered.groupBy { it.category }
            .map { (name, list) ->
                val amt = list.sumOf { it.amount }
                val color = categories.firstOrNull { it.name == name }?.colorHex ?: "#6C5DD3"
                CategorySlice(name, amt, color, if (total > 0) amt / total * 100 else 0.0)
            }
            .sortedByDescending { it.amount }
    }
}
