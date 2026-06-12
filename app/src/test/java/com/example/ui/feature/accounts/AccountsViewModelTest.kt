package com.example.ui.feature.accounts

import com.example.domain.FakeAccountRepository
import com.example.domain.FakeTransactionRepository
import com.example.domain.model.Account
import com.example.domain.model.AccountType
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

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {
    private val accountRepo = FakeAccountRepository()
    private val txnRepo = FakeTransactionRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `balances reflect opening balance plus income, expense, and both legs of transfer`() = runTest {
        accountRepo.upsert(Account(1, "Cash", AccountType.CASH, 1000.0, "wallet", "#0f0"))
        accountRepo.upsert(Account(2, "Bank", AccountType.BANK, 5000.0, "bank", "#00f"))
        txnRepo.upsert(Transaction(0, TxnType.INCOME, 2000.0, "Salary", 2, null, 0, ""))
        txnRepo.upsert(Transaction(0, TxnType.EXPENSE, 300.0, "Food", 1, null, 0, ""))
        txnRepo.upsert(Transaction(0, TxnType.TRANSFER, 500.0, "Move", 2, 1, 0, ""))

        val s = AccountsViewModel(accountRepo, txnRepo).uiState.first()
        val cash = s.balances.first { it.account.id == 1L }.balance
        val bank = s.balances.first { it.account.id == 2L }.balance
        assertEquals(1000.0 - 300.0 + 500.0, cash, 0.0)
        assertEquals(5000.0 + 2000.0 - 500.0, bank, 0.0)
        assertEquals(cash + bank, s.totalNet, 0.0)
    }
}
