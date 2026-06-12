package com.example.ui.feature.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datetime.RangeBounds
import com.example.domain.model.Budget
import com.example.domain.model.SpendingStats
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.BudgetRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.TransactionRepository
import com.example.domain.usecase.CategoryBreakdownUseCase
import com.example.domain.usecase.MonthlySummaryUseCase
import com.example.domain.usecase.PaymentModeBreakdownUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    txnRepo: TransactionRepository,
    catRepo: CategoryRepository,
    accountRepo: AccountRepository,
    budgetRepo: BudgetRepository
) : ViewModel() {

    private val summaryUseCase = MonthlySummaryUseCase()
    private val categoryBreakdown = CategoryBreakdownUseCase()
    private val paymentBreakdown = PaymentModeBreakdownUseCase()

    private val period = MutableStateFlow(AnalysisPeriod.MONTH)
    private val anchor = MutableStateFlow(LocalDate.now())

    fun setPeriod(p: AnalysisPeriod) { period.value = p }
    fun next() { anchor.value = shift(anchor.value, period.value, +1) }
    fun prev() { anchor.value = shift(anchor.value, period.value, -1) }

    val uiState: StateFlow<AnalysisUiState> = combine(
        txnRepo.all(), catRepo.all(), accountRepo.all(), budgetRepo.all(),
        combine(period, anchor) { p, a -> p to a }
    ) { txns, cats, accounts, budgets, (p, a) ->
        val (start, end) = boundsFor(p, a)
        val inRange = txns.filter { it.date in start..end }
        AnalysisUiState(
            period = p,
            rangeTitle = title(p, a),
            txnCount = inRange.size,
            summary = summaryUseCase.from(inRange),
            budget = budgets.firstOrNull { it.key == "%04d-%02d".format(a.year, a.monthValue) },
            categorySpending = categoryBreakdown.from(inRange, cats, TxnType.EXPENSE),
            categoryIncome = categoryBreakdown.from(inRange, cats, TxnType.INCOME),
            paymentSpending = paymentBreakdown.from(inRange, accounts, TxnType.EXPENSE),
            paymentIncome = paymentBreakdown.from(inRange, accounts, TxnType.INCOME),
            paymentTransfer = paymentBreakdown.from(inRange, accounts, TxnType.TRANSFER),
            stats = statsFor(inRange, start, end)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalysisUiState())

    private fun boundsFor(p: AnalysisPeriod, a: LocalDate) = when (p) {
        AnalysisPeriod.WEEK -> RangeBounds.week(a)
        AnalysisPeriod.YEAR -> RangeBounds.year(a)
        else -> RangeBounds.month(a) // MONTH + CUSTOM (custom UI deferred)
    }

    private fun shift(a: LocalDate, p: AnalysisPeriod, dir: Int): LocalDate = when (p) {
        AnalysisPeriod.WEEK -> a.plusWeeks(dir.toLong())
        AnalysisPeriod.YEAR -> a.plusYears(dir.toLong())
        else -> a.plusMonths(dir.toLong())
    }

    private fun title(p: AnalysisPeriod, a: LocalDate): String = when (p) {
        AnalysisPeriod.WEEK -> {
            val (s, e) = RangeBounds.week(a)
            "${fmtDay(s)} - ${fmtDay(e)}"
        }
        AnalysisPeriod.YEAR -> a.year.toString()
        else -> "${a.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${a.year}"
    }

    private fun fmtDay(millis: Long): String {
        val d = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return "${d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.dayOfMonth}"
    }

    /** Real-day average denominator (E#6) rather than a fixed 30. */
    private fun statsFor(txns: List<Transaction>, start: Long, end: Long): SpendingStats {
        val exp = txns.filter { it.type == TxnType.EXPENSE }
        val inc = txns.filter { it.type == TxnType.INCOME }
        val spend = exp.sumOf { it.amount }
        val days = RangeBounds.dayCount(start, end)
        return SpendingStats(
            avgPerDay = spend / days,
            avgPerTxn = if (exp.isNotEmpty()) spend / exp.size else 0.0,
            txnCount = exp.size,
            avgIncomePerTxn = if (inc.isNotEmpty()) inc.sumOf { it.amount } / inc.size else 0.0
        )
    }
}
