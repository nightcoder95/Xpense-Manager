package com.example.domain.usecase

import com.example.domain.model.Account
import com.example.domain.model.AccountBalance
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType

class AccountBalancesUseCase {
    fun from(accounts: List<Account>, txns: List<Transaction>): List<AccountBalance> =
        accounts.map { acc ->
            var bal = acc.openingBalance
            txns.forEach { t ->
                when (t.type) {
                    TxnType.INCOME -> if (t.accountId == acc.id) bal += t.amount
                    TxnType.EXPENSE -> if (t.accountId == acc.id) bal -= t.amount
                    TxnType.TRANSFER -> {
                        if (t.accountId == acc.id) bal -= t.amount
                        if (t.toAccountId == acc.id) bal += t.amount
                    }
                }
            }
            AccountBalance(acc, bal)
        }
}
