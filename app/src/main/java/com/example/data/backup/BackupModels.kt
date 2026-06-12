package com.example.data.backup

import com.squareup.moshi.JsonClass

/**
 * Full-backup wire format. Mirrors the domain models but stores enums as plain strings so the
 * JSON stays human-readable and resilient to enum reordering. Bump [BackupData.version] if the
 * shape changes incompatibly.
 */
@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val accounts: List<BackupAccount> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    val transactions: List<BackupTransaction> = emptyList(),
    val budgets: List<BackupBudget> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BackupAccount(
    val id: Long,
    val name: String,
    val type: String,
    val openingBalance: Double,
    val iconName: String,
    val colorHex: String,
    val archived: Boolean = false
)

@JsonClass(generateAdapter = true)
data class BackupCategory(
    val name: String,
    val type: String,
    val iconName: String,
    val colorHex: String,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
)

@JsonClass(generateAdapter = true)
data class BackupTransaction(
    val id: Long,
    val type: String,
    val amount: Double,
    val category: String,
    val accountId: Long,
    val toAccountId: Long? = null,
    val date: Long,
    val note: String,
    val tag: String = ""
)

@JsonClass(generateAdapter = true)
data class BackupBudget(
    val key: String,
    val amountLimit: Double,
    val period: String = "MONTHLY"
)
