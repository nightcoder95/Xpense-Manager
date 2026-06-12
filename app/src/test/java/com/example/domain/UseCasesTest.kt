package com.example.domain

import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType
import com.example.domain.usecase.AccountBalancesUseCase
import com.example.domain.usecase.MonthlySummaryUseCase
import com.example.domain.usecase.SaveTransactionUseCase
import com.example.domain.usecase.SearchTransactionsUseCase
import com.example.domain.usecase.SpendingStatsUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class UseCasesTest {
    private val txns = listOf(
        Transaction(1, TxnType.INCOME, 12000.0, "Salary", 2, null, day(2026, 6, 7), ""),
        Transaction(2, TxnType.EXPENSE, 685.0, "Bills and Utilities", 1, null, day(2026, 6, 11), ""),
        Transaction(3, TxnType.EXPENSE, 268.0, "Food and Dining", 1, null, day(2026, 6, 11), "")
    )

    @Test fun summary_income_spending_net() {
        val s = MonthlySummaryUseCase().from(txns)
        assertEquals(12000.0, s.income, 0.0)
        assertEquals(953.0, s.spending, 0.0)
        assertEquals(11047.0, s.net, 0.0)
    }

    @Test fun stats_perDay_usesRealDayCount() {
        val stats = SpendingStatsUseCase().from(txns, year = 2026, month = 6)
        assertEquals(953.0 / 30.0, stats.avgPerDay, 0.001) // June = 30 days
        assertEquals(2, stats.txnCount)
    }

    @Test fun save_rejectsZeroAmount() {
        val r = SaveTransactionUseCase.validate(0.0, TxnType.EXPENSE, accountId = 1, toAccountId = null)
        assertEquals(false, r.isValid)
    }

    @Test fun save_rejectsTransferToSameAccount() {
        val r = SaveTransactionUseCase.validate(100.0, TxnType.TRANSFER, accountId = 1, toAccountId = 1)
        assertEquals(false, r.isValid)
    }

    @Test fun save_acceptsValidExpense() {
        val r = SaveTransactionUseCase.validate(100.0, TxnType.EXPENSE, accountId = 1, toAccountId = null)
        assertEquals(true, r.isValid)
    }

    @Test fun balances_includeTransfers() {
        val accounts = listOf(
            Account(1, "Cash", AccountType.CASH, 1000.0, "cash", "#43C59E"),
            Account(2, "Bank", AccountType.BANK, 5000.0, "bank", "#6C5DD3")
        )
        val t = listOf(Transaction(9, TxnType.TRANSFER, 200.0, "", 1, 2, day(2026, 6, 1), ""))
        val balances = AccountBalancesUseCase().from(accounts, t).associateBy { it.account.id }
        assertEquals(800.0, balances[1]!!.balance, 0.0)   // cash out
        assertEquals(5200.0, balances[2]!!.balance, 0.0)  // bank in
    }

    @Test fun search_filtersByQueryCategoryAccount() {
        val result = SearchTransactionsUseCase().filter(txns, "food", emptySet(), emptySet())
        assertEquals(1, result.size)
        assertEquals("Food and Dining", result.first().category)
    }
}
