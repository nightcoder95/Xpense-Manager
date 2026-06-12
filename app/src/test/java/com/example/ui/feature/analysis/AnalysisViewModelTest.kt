package com.example.ui.feature.analysis

import com.example.domain.FakeAccountRepository
import com.example.domain.FakeBudgetRepository
import com.example.domain.FakeCategoryRepository
import com.example.domain.FakeTransactionRepository
import com.example.domain.day
import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {
    private val txnRepo = FakeTransactionRepository()
    private val catRepo = FakeCategoryRepository()
    private val accountRepo = FakeAccountRepository()
    private val budgetRepo = FakeBudgetRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = AnalysisViewModel(txnRepo, catRepo, accountRepo, budgetRepo)

    @Test fun `month period aggregates summary, breakdowns and per-day stat over real days`() = runTest {
        val now = LocalDate.now()
        accountRepo.upsert(Account(1, "Cash", AccountType.CASH, 0.0, "wallet", "#0f0"))
        catRepo.upsert(Category("Food", TxnType.EXPENSE, "food", "#f00"))
        txnRepo.upsert(Transaction(0, TxnType.EXPENSE, 300.0, "Food", 1, null, day(now.year, now.monthValue, 5), ""))
        txnRepo.upsert(Transaction(0, TxnType.INCOME, 9000.0, "Salary", 1, null, day(now.year, now.monthValue, 6), ""))

        val s = vm().uiState.first()
        assertEquals(300.0, s.summary.spending, 0.0)
        assertEquals(9000.0, s.summary.income, 0.0)
        assertEquals(2, s.txnCount)
        assertEquals(1, s.categorySpending.size)
        assertEquals(1, s.paymentSpending.size)
        val days = now.lengthOfMonth()
        assertEquals(300.0 / days, s.stats.avgPerDay, 0.001)
    }

    @Test fun `empty month gives zero stats and empty slices`() = runTest {
        val s = vm().uiState.first()
        assertEquals(0, s.txnCount)
        assertEquals(0.0, s.stats.avgPerDay, 0.0)
        assertEquals(0, s.categorySpending.size)
    }
}
