package com.example.ui.feature.home

import com.example.domain.FakeAccountRepository
import com.example.domain.FakeBudgetRepository
import com.example.domain.FakeCategoryRepository
import com.example.domain.FakeTransactionRepository
import com.example.domain.day
import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.domain.model.Budget
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val txnRepo = FakeTransactionRepository()
    private val catRepo = FakeCategoryRepository()
    private val budgetRepo = FakeBudgetRepository()
    private val accountRepo = FakeAccountRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = HomeViewModel(txnRepo, catRepo, budgetRepo, accountRepo)

    @Test fun `greeting buckets by hour`() {
        assertEquals("Good Morning", greetingForHour(9))
        assertEquals("Good Afternoon", greetingForHour(14))
        assertEquals("Good Evening", greetingForHour(19))
        assertEquals("Good Night", greetingForHour(2))
    }

    @Test fun `empty month yields zero summary and empty recent`() = runTest {
        val s = vm().uiState.first()
        assertEquals(0.0, s.summary.income, 0.0)
        assertEquals(0.0, s.summary.spending, 0.0)
        assertEquals(0, s.recent.size)
        assertNull(s.budget)
    }

    @Test fun `summary, recent take 3 newest, and budget resolve`() = runTest {
        val now = LocalDate.now()
        accountRepo.upsert(Account(1, "Cash", AccountType.CASH, 0.0, "wallet", "#fff"))
        catRepo.upsert(Category("Food", TxnType.EXPENSE, "food", "#f00", isDefault = false))
        budgetRepo.upsert(Budget("%04d-%02d".format(now.year, now.monthValue), 5000.0))
        listOf(1, 2, 3, 4).forEach { d ->
            txnRepo.upsert(Transaction(0, TxnType.EXPENSE, d * 100.0, "Food", 1, null, day(now.year, now.monthValue, d), ""))
        }
        txnRepo.upsert(Transaction(0, TxnType.INCOME, 9000.0, "Salary", 1, null, day(now.year, now.monthValue, 2), ""))

        val s = vm().uiState.first()
        assertEquals(9000.0, s.summary.income, 0.0)
        assertEquals(1000.0, s.summary.spending, 0.0)
        assertEquals(3, s.recent.size)
        assertEquals(5000.0, s.budget?.amountLimit)
        assertEquals(3, s.setupDone) // custom category + accounts + transactions
    }
}
