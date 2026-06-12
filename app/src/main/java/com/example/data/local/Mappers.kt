package com.example.data.local

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.domain.model.Budget
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType

fun TransactionEntity.toDomain() = Transaction(
    id, TxnType.valueOf(type), amount, category, accountId, toAccountId, date, note, tag
)

fun Transaction.toEntity() = TransactionEntity(
    id, type.name, amount, category, accountId, toAccountId, date, note, tag
)

fun CategoryEntity.toDomain() = Category(name, TxnType.valueOf(type), iconName, colorHex, sortOrder, isDefault)
fun Category.toEntity() = CategoryEntity(name, type.name, iconName, colorHex, sortOrder, isDefault)

fun AccountEntity.toDomain() = Account(id, name, AccountType.valueOf(type), openingBalance, iconName, colorHex, archived)
fun Account.toEntity() = AccountEntity(id, name, type.name, openingBalance, iconName, colorHex, archived)

fun BudgetEntity.toDomain() = Budget(key, amountLimit, BudgetPeriod.valueOf(period))
fun Budget.toEntity() = BudgetEntity(key, amountLimit, period.name)
