package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.AppDispatchers
import com.example.domain.model.Account
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.TxnType
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.BudgetRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.TransactionRepository
import com.example.domain.usecase.SaveTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.domain.model.Transaction
import com.example.domain.model.Category
import com.example.domain.model.Transaction as DomainTransaction
import com.example.domain.model.Category as DomainCategory

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val txnRepo: TransactionRepository,
    private val catRepo: CategoryRepository,
    private val budgetRepo: BudgetRepository,
    private val accountRepo: AccountRepository,
    private val saveTransaction: SaveTransactionUseCase,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepo.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = txnRepo.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = catRepo.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // One-shot error surface (replaces swallowed exceptions).
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors = _errors.asSharedFlow()

    // --- UI state ---
    private val _selectedTab = MutableStateFlow(AppTab.HOME)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _viewYear = MutableStateFlow(2026)
    val viewYear: StateFlow<Int> = _viewYear.asStateFlow()

    private val _viewMonth = MutableStateFlow(6)
    val viewMonth: StateFlow<Int> = _viewMonth.asStateFlow()

    private val _editingTransaction = MutableStateFlow<Transaction?>(null)
    val editingTransaction: StateFlow<Transaction?> = _editingTransaction.asStateFlow()

    /** Account name for a transaction's accountId — used by the Add/Edit form's payment picker. */
    fun accountName(accountId: Long): String =
        accounts.value.firstOrNull { it.id == accountId }?.name ?: ""

    private val _isAddSheetOpen = MutableStateFlow(false)
    val isAddSheetOpen: StateFlow<Boolean> = _isAddSheetOpen.asStateFlow()

    private val _isCategoryScreenOpen = MutableStateFlow(false)
    val isCategoryScreenOpen: StateFlow<Boolean> = _isCategoryScreenOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterCategory = MutableStateFlow<String?>(null)
    val filterCategory: StateFlow<String?> = _filterCategory.asStateFlow()

    private val _filterPaymentMode = MutableStateFlow<String?>(null)
    val filterPaymentMode: StateFlow<String?> = _filterPaymentMode.asStateFlow()

    // --- navigation / view state ---
    fun selectTab(tab: AppTab) { _selectedTab.value = tab }

    fun nextMonth() {
        if (_viewMonth.value == 12) { _viewMonth.value = 1; _viewYear.value += 1 }
        else _viewMonth.value += 1
    }

    fun prevMonth() {
        if (_viewMonth.value == 1) { _viewMonth.value = 12; _viewYear.value -= 1 }
        else _viewMonth.value -= 1
    }

    fun openAddTransaction(transactionToEdit: DomainTransaction? = null) {
        _editingTransaction.value = transactionToEdit
        _isAddSheetOpen.value = true
    }

    fun closeAddTransaction() {
        _editingTransaction.value = null
        _isAddSheetOpen.value = false
    }

    fun setCategoryScreenOpen(open: Boolean) { _isCategoryScreenOpen.value = open }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun setFilterCategory(categoryName: String?) { _filterCategory.value = categoryName }
    fun setFilterPaymentMode(modeName: String?) { _filterPaymentMode.value = modeName }

    // --- writes (routed through use cases / repositories, errors surfaced) ---
    private fun accountIdForName(name: String): Long =
        accounts.value.firstOrNull { it.name == name }?.id
            ?: accounts.value.firstOrNull()?.id
            ?: 1L

    fun saveTransaction(
        type: String,
        amount: Double,
        category: String,
        paymentMode: String,
        date: Long,
        note: String,
        tag: String
    ) {
        val editing = _editingTransaction.value
        val domain = DomainTransaction(
            id = editing?.id ?: 0L,
            type = runCatching { TxnType.valueOf(type) }.getOrDefault(TxnType.EXPENSE),
            amount = amount,
            category = category,
            accountId = accountIdForName(paymentMode),
            toAccountId = null,
            date = date,
            note = note,
            tag = tag
        )
        viewModelScope.launch {
            saveTransaction(domain)
                .onSuccess { closeAddTransaction() }
                .onFailure { _errors.tryEmit(it.message ?: "Could not save transaction") }
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            runCatching { txnRepo.delete(transactionId) }
                .onFailure { _errors.tryEmit("Could not delete transaction") }
        }
    }

    fun saveBudget(yearMonth: String, limit: Double) {
        viewModelScope.launch {
            runCatching { budgetRepo.upsert(com.example.domain.model.Budget(yearMonth, limit, BudgetPeriod.MONTHLY)) }
                .onFailure { _errors.tryEmit("Could not save budget") }
        }
    }

    fun createCategory(name: String, type: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            runCatching {
                catRepo.upsert(
                    DomainCategory(name, runCatching { TxnType.valueOf(type) }.getOrDefault(TxnType.EXPENSE), iconName, colorHex)
                )
            }.onFailure { _errors.tryEmit("Could not create category") }
        }
    }

    fun deleteCategory(category: DomainCategory) {
        viewModelScope.launch {
            runCatching { catRepo.delete(category) }
                .onFailure { _errors.tryEmit("Could not delete category") }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            runCatching { txnRepo.deleteAll() }
                .onFailure { _errors.tryEmit("Could not clear data") }
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            runCatching {
                txnRepo.deleteAll()
                val ts: (Int, Int) -> Long = { day, month ->
                    com.example.core.datetime.MonthRange.bounds(2026, month).first +
                        (day - 1).toLong() * 24 * 60 * 60 * 1000
                }
                val demo = listOf(
                    DemoTxn("INCOME", 85000.0, "Salary", "Bank Account", ts(1, 5), "Monthly salary credit"),
                    DemoTxn("EXPENSE", 18000.0, "Rent", "Bank Account", ts(2, 5), "Apartment 4B Rent"),
                    DemoTxn("EXPENSE", 1250.0, "Food and Dining", "Credit Card", ts(5, 5), "Weekend Sushi dinner"),
                    DemoTxn("EXPENSE", 3200.0, "Bills and Utilities", "Credit Card", ts(10, 5), "Electricity & Fiber Internet"),
                    DemoTxn("EXPENSE", 4500.0, "Travelling", "Cash", ts(12, 5), "Weekend getaway fuel & tolls"),
                    DemoTxn("EXPENSE", 6800.0, "Shopping", "Credit Card", ts(18, 5), "Premium noise-canceling headphones"),
                    DemoTxn("EXPENSE", 450.0, "Food and Dining", "Cash", ts(20, 5), "Starbucks coffee & bagels"),
                    DemoTxn("EXPENSE", 1199.0, "Entertainment", "Credit Card", ts(22, 5), "Netflix & Spotify annual renewal"),
                    DemoTxn("EXPENSE", 2300.0, "Shopping", "Cash", ts(28, 5), "Casual leather shoes"),
                    DemoTxn("INCOME", 85000.0, "Salary", "Bank Account", ts(1, 6), "Monthly salary credit"),
                    DemoTxn("EXPENSE", 18000.0, "Rent", "Bank Account", ts(2, 6), "Apartment 4B Rent"),
                    DemoTxn("EXPENSE", 890.0, "Food and Dining", "Credit Card", ts(3, 6), "Lunch with project team"),
                    DemoTxn("EXPENSE", 2800.0, "Bills and Utilities", "Bank Account", ts(5, 6), "Monthly cellular & water bills"),
                    DemoTxn("EXPENSE", 1500.0, "Shopping", "Credit Card", ts(7, 6), "Desk organizer and notebook"),
                    DemoTxn("EXPENSE", 800.0, "Travelling", "Cash", ts(8, 6), "Metro transit cards reload"),
                    DemoTxn("EXPENSE", 1200.0, "Personal Care", "Credit Card", ts(10, 6), "Haircut & grooming essentials"),
                    DemoTxn("EXPENSE", 340.0, "Food and Dining", "Cash", ts(11, 6), "Morning flat white & croissant")
                )
                demo.forEach { d ->
                    txnRepo.upsert(
                        DomainTransaction(
                            id = 0L,
                            type = TxnType.valueOf(d.type),
                            amount = d.amount,
                            category = d.category,
                            accountId = accountIdForName(d.paymentMode),
                            toAccountId = null,
                            date = d.date,
                            note = d.note,
                            tag = ""
                        )
                    )
                }
                budgetRepo.upsert(com.example.domain.model.Budget("2026-05", 45000.0, BudgetPeriod.MONTHLY))
                budgetRepo.upsert(com.example.domain.model.Budget("2026-06", 50000.0, BudgetPeriod.MONTHLY))
            }.onFailure { _errors.tryEmit("Could not seed demo data") }
        }
    }

    private data class DemoTxn(
        val type: String, val amount: Double, val category: String,
        val paymentMode: String, val date: Long, val note: String
    )
}

enum class AppTab {
    HOME, ANALYSIS, TRANSACTIONS, MORE
}
