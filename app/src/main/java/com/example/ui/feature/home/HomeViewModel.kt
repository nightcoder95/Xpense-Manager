package com.example.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datetime.MonthRange
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.BudgetRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.TransactionRepository
import com.example.domain.usecase.MonthlySummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    txnRepo: TransactionRepository,
    catRepo: CategoryRepository,
    budgetRepo: BudgetRepository,
    accountRepo: AccountRepository
) : ViewModel() {

    private val summaryUseCase = MonthlySummaryUseCase()
    private val today = LocalDate.now()
    private val bounds = MonthRange.bounds(today.year, today.monthValue)
    private val budgetKey = "%04d-%02d".format(today.year, today.monthValue)

    val uiState: StateFlow<HomeUiState> = combine(
        txnRepo.inRange(bounds.first, bounds.second),
        catRepo.all(),
        budgetRepo.all(),
        accountRepo.all()
    ) { txns, cats, budgets, accounts ->
        val recent = txns.sortedByDescending { it.date }.take(3)
        val setupDone = listOf(
            cats.any { !it.isDefault },
            accounts.isNotEmpty(),
            txns.isNotEmpty()
        ).count { it }
        HomeUiState(
            greeting = greetingForHour(LocalTime.now().hour),
            summary = summaryUseCase.from(txns),
            budget = budgets.firstOrNull { it.key == budgetKey },
            recent = recent,
            setupDone = setupDone,
            categories = cats,
            accountNames = accounts.associate { it.id to it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
