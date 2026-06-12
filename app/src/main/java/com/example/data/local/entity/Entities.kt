package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val openingBalance: Double,
    val iconName: String,
    val colorHex: String,
    val archived: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("accountId"), Index("category")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Double,
    val category: String,
    val accountId: Long,
    val toAccountId: Long? = null,
    val date: Long,
    val note: String,
    val tag: String = ""
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val type: String,
    val iconName: String,
    val colorHex: String,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val key: String,
    val amountLimit: Double,
    val period: String = "MONTHLY"
)
