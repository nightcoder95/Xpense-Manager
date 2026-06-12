package com.example.domain.repository

import com.example.domain.model.Account
import com.example.domain.model.Budget
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun all(): Flow<List<Transaction>>
    fun inRange(start: Long, end: Long): Flow<List<Transaction>>
    suspend fun upsert(t: Transaction): Long
    suspend fun update(t: Transaction)
    suspend fun delete(id: Long)
    suspend fun deleteAll()
    suspend fun getById(id: Long): Transaction?
    suspend fun countByAccount(accountId: Long): Int
}

interface CategoryRepository {
    fun all(): Flow<List<Category>>
    suspend fun upsert(c: Category)
    suspend fun delete(c: Category)
    suspend fun deleteAll()
    suspend fun count(): Int
    suspend fun seedDefaults(list: List<Category>)
}

interface AccountRepository {
    fun all(): Flow<List<Account>>
    suspend fun getAllOnce(): List<Account>
    suspend fun getById(id: Long): Account?
    suspend fun count(): Int
    suspend fun upsert(a: Account): Long
    suspend fun delete(id: Long)
    suspend fun deleteAll()
    suspend fun seedDefaults(list: List<Account>)
}

interface BudgetRepository {
    fun all(): Flow<List<Budget>>
    suspend fun getByKey(key: String): Budget?
    suspend fun upsert(b: Budget)
    suspend fun delete(key: String)
    suspend fun deleteAll()
}
