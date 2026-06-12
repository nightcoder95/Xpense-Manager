package com.example.ui.feature.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datetime.RangeBounds
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.TransactionRepository
import com.example.domain.usecase.CalendarDay
import com.example.domain.usecase.DayView
import com.example.domain.usecase.GetCalendarMonthUseCase
import com.example.domain.usecase.GetDayViewUseCase
import com.example.domain.usecase.MonthlySummaryUseCase
import com.example.domain.usecase.SearchTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class CalendarState(val anchor: LocalDate, val days: Map<Int, CalendarDay>)

data class CustomFilter(
    val query: String = "",
    val categories: Set<String> = emptySet(),
    val accountIds: Set<Long> = emptySet()
)

data class CustomState(
    val filter: CustomFilter = CustomFilter(),
    val results: List<Transaction> = emptyList(),
    val summary: com.example.domain.model.MonthlySummary = com.example.domain.model.MonthlySummary(0.0, 0.0),
    val allCategories: List<Category> = emptyList(),
    val allAccounts: List<Account> = emptyList()
)

@HiltViewModel
class ViewsViewModel @Inject constructor(
    private val txnRepo: TransactionRepository,
    catRepo: CategoryRepository,
    accountRepo: AccountRepository
) : ViewModel() {

    private val dayUseCase = GetDayViewUseCase()
    private val calendarUseCase = GetCalendarMonthUseCase()
    private val search = SearchTransactionsUseCase()
    private val summaryUseCase = MonthlySummaryUseCase()

    private val dayAnchor = MutableStateFlow(LocalDate.now())
    private val monthAnchor = MutableStateFlow(LocalDate.now())
    private val filter = MutableStateFlow(CustomFilter())

    fun dayPrev() { dayAnchor.value = dayAnchor.value.minusDays(1) }
    fun dayNext() { dayAnchor.value = dayAnchor.value.plusDays(1) }
    fun monthPrev() { monthAnchor.value = monthAnchor.value.minusMonths(1) }
    fun monthNext() { monthAnchor.value = monthAnchor.value.plusMonths(1) }
    fun openDay(date: LocalDate) { dayAnchor.value = date }
    fun setFilter(f: CustomFilter) { filter.value = f }
    fun setQuery(q: String) { filter.value = filter.value.copy(query = q) }

    /** Category list + account-name map, shared by all three views for row rendering. */
    val meta: StateFlow<Pair<List<Category>, Map<Long, String>>> =
        combine(catRepo.all(), accountRepo.all()) { cats, accounts ->
            cats to accounts.associate { it.id to it.name }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Category>() to emptyMap())

    val dayState: StateFlow<Pair<LocalDate, DayView>> =
        combine(txnRepo.all(), dayAnchor) { txns, anchor ->
            val (s, e) = RangeBounds.day(anchor)
            anchor to dayUseCase.from(txns, s, e)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDate.now() to DayView(emptyList(), com.example.domain.model.MonthlySummary(0.0, 0.0)))

    val calendarState: StateFlow<CalendarState> =
        combine(txnRepo.all(), monthAnchor) { txns, anchor ->
            val (s, e) = RangeBounds.month(anchor)
            CalendarState(anchor, calendarUseCase.from(txns, s, e))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarState(LocalDate.now(), emptyMap()))

    val customState: StateFlow<CustomState> =
        combine(txnRepo.all(), catRepo.all(), accountRepo.all(), filter) { txns, cats, accounts, f ->
            val results = search.filter(txns, f.query, f.categories, f.accountIds).sortedByDescending { it.date }
            CustomState(f, results, summaryUseCase.from(results), cats, accounts)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomState())
}
