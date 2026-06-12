package com.example.ui.feature.home

import com.example.domain.model.Budget
import com.example.domain.model.Category
import com.example.domain.model.MonthlySummary
import com.example.domain.model.Transaction

/** Range-scoped Home state. All values are pre-computed by [HomeViewModel] (no in-composable filtering). */
data class HomeUiState(
    val greeting: String = "",
    val summary: MonthlySummary = MonthlySummary(0.0, 0.0),
    val budget: Budget? = null,
    val recent: List<Transaction> = emptyList(),
    val setupDone: Int = 0,
    val categories: List<Category> = emptyList(),
    val accountNames: Map<Long, String> = emptyMap()
)

/** Time-of-day greeting. Pure + testable. */
fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    in 17..20 -> "Good Evening"
    else -> "Good Night"
}
