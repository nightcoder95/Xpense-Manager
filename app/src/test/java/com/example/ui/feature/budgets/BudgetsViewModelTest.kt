package com.example.ui.feature.budgets

import com.example.domain.FakeBudgetRepository
import com.example.domain.FakeCategoryRepository
import com.example.domain.FakeTransactionRepository
import com.example.domain.day
import com.example.domain.model.BudgetPeriod
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
class BudgetsViewModelTest {
    private val budgetRepo = FakeBudgetRepository()
    private val txnRepo = FakeTransactionRepository()
    private val catRepo = FakeCategoryRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `saving monthly budget surfaces it with spend and category slices`() = runTest {
        val now = LocalDate.now()
        catRepo.upsert(Category("Food", TxnType.EXPENSE, "food", "#f00"))
        txnRepo.upsert(Transaction(0, TxnType.EXPENSE, 400.0, "Food", 1, null, day(now.year, now.monthValue, 5), ""))

        val vm = BudgetsViewModel(budgetRepo, txnRepo, catRepo)
        vm.saveBudget(5000.0)

        val s = vm.uiState.first()
        assertEquals(BudgetPeriod.MONTHLY, s.period)
        assertEquals(5000.0, s.limit)
        assertEquals(400.0, s.spent, 0.0)
        assertEquals(1, s.categories.size)
    }
}
