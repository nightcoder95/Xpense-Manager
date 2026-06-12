package com.example.ui.feature.analysis

import com.example.domain.model.Budget
import com.example.domain.model.CategorySlice
import com.example.domain.model.MonthlySummary
import com.example.domain.model.SpendingStats

enum class AnalysisPeriod { WEEK, MONTH, YEAR, CUSTOM }

data class AnalysisUiState(
    val period: AnalysisPeriod = AnalysisPeriod.MONTH,
    val rangeTitle: String = "",
    val txnCount: Int = 0,
    val summary: MonthlySummary = MonthlySummary(0.0, 0.0),
    val budget: Budget? = null,
    val categorySpending: List<CategorySlice> = emptyList(),
    val categoryIncome: List<CategorySlice> = emptyList(),
    val paymentSpending: List<CategorySlice> = emptyList(),
    val paymentIncome: List<CategorySlice> = emptyList(),
    val paymentTransfer: List<CategorySlice> = emptyList(),
    val stats: SpendingStats = SpendingStats(0.0, 0.0, 0, 0.0)
)
