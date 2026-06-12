package com.example.ui.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datetime.RangeBounds
import com.example.domain.model.Budget
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.CategorySlice
import com.example.domain.model.TxnType
import com.example.domain.repository.BudgetRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.TransactionRepository
import com.example.domain.usecase.CategoryBreakdownUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BudgetsUiState(
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val key: String = "",
    val limit: Double? = null,
    val spent: Double = 0.0,
    val categories: List<CategorySlice> = emptyList()
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepo: BudgetRepository,
    private val txnRepo: TransactionRepository,
    catRepo: CategoryRepository
) : ViewModel() {

    private val breakdown = CategoryBreakdownUseCase()
    private val period = MutableStateFlow(BudgetPeriod.MONTHLY)
    private val today = LocalDate.now()

    fun setPeriod(p: BudgetPeriod) { period.value = p }

    private fun keyFor(p: BudgetPeriod) =
        if (p == BudgetPeriod.ANNUAL) "%04d".format(today.year) else "%04d-%02d".format(today.year, today.monthValue)

    private fun boundsFor(p: BudgetPeriod) =
        if (p == BudgetPeriod.ANNUAL) RangeBounds.year(today) else RangeBounds.month(today)

    val uiState: StateFlow<BudgetsUiState> =
        combine(budgetRepo.all(), txnRepo.all(), catRepo.all(), period) { budgets, txns, cats, p ->
            val key = keyFor(p)
            val (start, end) = boundsFor(p)
            val inRange = txns.filter { it.date in start..end }
            val slices = breakdown.from(inRange, cats, TxnType.EXPENSE)
            BudgetsUiState(
                period = p,
                key = key,
                limit = budgets.firstOrNull { it.key == key }?.amountLimit,
                spent = slices.sumOf { it.amount },
                categories = slices
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetsUiState())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    fun saveBudget(limit: Double) {
        val p = period.value
        viewModelScope.launch {
            runCatching { budgetRepo.upsert(Budget(keyFor(p), limit, p)) }
                .onFailure { _messages.tryEmit("Could not save budget") }
        }
    }

    fun deleteBudget() {
        val p = period.value
        viewModelScope.launch {
            runCatching { budgetRepo.delete(keyFor(p)) }
                .onFailure { _messages.tryEmit("Could not remove budget") }
        }
    }
}
