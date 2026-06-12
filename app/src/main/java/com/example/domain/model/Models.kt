package com.example.domain.model

enum class TxnType { EXPENSE, INCOME, TRANSFER }
enum class AccountType { CASH, BANK, CREDIT_CARD, WALLET }
enum class BudgetPeriod { MONTHLY, ANNUAL }

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val openingBalance: Double,
    val iconName: String,
    val colorHex: String,
    val archived: Boolean = false
)

data class Category(
    val name: String,
    val type: TxnType,
    val iconName: String,
    val colorHex: String,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
)

data class Transaction(
    val id: Long,
    val type: TxnType,
    val amount: Double,
    val category: String,
    val accountId: Long,
    val toAccountId: Long? = null,
    val date: Long,
    val note: String,
    val tag: String = ""
)

data class Budget(val key: String, val amountLimit: Double, val period: BudgetPeriod = BudgetPeriod.MONTHLY)

data class MonthlySummary(val income: Double, val spending: Double) {
    val net: Double get() = income - spending
}

data class CategorySlice(val category: String, val amount: Double, val colorHex: String, val percent: Double)

data class AccountBalance(val account: Account, val balance: Double)

data class SpendingStats(
    val avgPerDay: Double,
    val avgPerTxn: Double,
    val txnCount: Int,
    val avgIncomePerTxn: Double
)
