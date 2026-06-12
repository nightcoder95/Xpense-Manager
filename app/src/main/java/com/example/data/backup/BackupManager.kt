package com.example.data.backup

import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.domain.model.Budget
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.BudgetRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.TransactionRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Serializes the whole database to a single JSON document and restores it. Restore is a full
 * replace: existing transactions are cleared first, then every record from the backup is
 * re-inserted with its original id so account/category references stay intact.
 */
class BackupManager @Inject constructor(
    private val txnRepo: TransactionRepository,
    private val catRepo: CategoryRepository,
    private val accRepo: AccountRepository,
    private val budgetRepo: BudgetRepository
) {
    private val adapter = Moshi.Builder().build().adapter(BackupData::class.java).indent("  ")

    suspend fun export(): String {
        val data = BackupData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            accounts = accRepo.getAllOnce().map {
                BackupAccount(it.id, it.name, it.type.name, it.openingBalance, it.iconName, it.colorHex, it.archived)
            },
            categories = catRepo.all().first().map {
                BackupCategory(it.name, it.type.name, it.iconName, it.colorHex, it.sortOrder, it.isDefault)
            },
            transactions = txnRepo.all().first().map {
                BackupTransaction(it.id, it.type.name, it.amount, it.category, it.accountId, it.toAccountId, it.date, it.note, it.tag)
            },
            budgets = budgetRepo.all().first().map {
                BackupBudget(it.key, it.amountLimit, it.period.name)
            }
        )
        return adapter.toJson(data)
    }

    /**
     * Full replace: every table is wiped first, then repopulated from the backup, so the device
     * ends up byte-for-byte equal to the backup with no leftover rows. Destructive — the caller
     * must confirm with the user first.
     *
     * @throws IllegalArgumentException if the JSON is malformed or not a recognized backup.
     */
    suspend fun import(json: String) {
        val data = adapter.fromJson(json) ?: throw IllegalArgumentException("Not a valid backup file")

        txnRepo.deleteAll()
        budgetRepo.deleteAll()
        catRepo.deleteAll()
        accRepo.deleteAll()

        data.accounts.forEach {
            accRepo.upsert(Account(it.id, it.name, AccountType.valueOf(it.type), it.openingBalance, it.iconName, it.colorHex, it.archived))
        }
        data.categories.forEach {
            catRepo.upsert(Category(it.name, TxnType.valueOf(it.type), it.iconName, it.colorHex, it.sortOrder, it.isDefault))
        }
        data.budgets.forEach {
            budgetRepo.upsert(Budget(it.key, it.amountLimit, BudgetPeriod.valueOf(it.period)))
        }
        data.transactions.forEach {
            txnRepo.upsert(Transaction(it.id, TxnType.valueOf(it.type), it.amount, it.category, it.accountId, it.toAccountId, it.date, it.note, it.tag))
        }
    }
}
