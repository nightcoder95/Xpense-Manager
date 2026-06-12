package com.example.domain

import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType
import com.example.domain.usecase.PaymentModeBreakdownUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentModeBreakdownUseCaseTest {
    private val uc = PaymentModeBreakdownUseCase()
    private val accounts = listOf(
        Account(1, "Cash", AccountType.CASH, 0.0, "wallet", "#0f0"),
        Account(2, "Bank", AccountType.BANK, 0.0, "bank", "#00f")
    )

    @Test fun `groups expenses by account, sorted desc, with percents`() {
        val txns = listOf(
            Transaction(1, TxnType.EXPENSE, 100.0, "x", 1, null, 0, ""),
            Transaction(2, TxnType.EXPENSE, 300.0, "y", 2, null, 0, ""),
            Transaction(3, TxnType.INCOME, 999.0, "z", 1, null, 0, "")
        )
        val out = uc.from(txns, accounts, TxnType.EXPENSE)
        assertEquals(2, out.size)
        assertEquals("Bank", out[0].category)
        assertEquals(300.0, out[0].amount, 0.0)
        assertEquals(75.0, out[0].percent, 0.001)
        assertEquals("Cash", out[1].category)
    }

    @Test fun `empty for type with no txns`() {
        assertEquals(0, uc.from(emptyList(), accounts, TxnType.TRANSFER).size)
    }
}
