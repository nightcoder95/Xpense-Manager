package com.example.domain

import com.example.domain.model.Account
import com.example.domain.model.Budget
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.BudgetRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.ZoneId

/** Epoch millis at noon on the given date. */
fun day(year: Int, month: Int, dayOfMonth: Int): Long =
    LocalDate.of(year, month, dayOfMonth)
        .atTime(12, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

class FakeTransactionRepository : TransactionRepository {
    private val items = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextId = 1L
    override fun all(): Flow<List<Transaction>> = items.asStateFlow()
    override fun inRange(start: Long, end: Long): Flow<List<Transaction>> = items.asStateFlow()
    override suspend fun upsert(t: Transaction): Long {
        val id = if (t.id == 0L) nextId++ else t.id
        items.value = items.value.filterNot { it.id == id } + t.copy(id = id)
        return id
    }
    override suspend fun update(t: Transaction) { upsert(t) }
    override suspend fun delete(id: Long) { items.value = items.value.filterNot { it.id == id } }
    override suspend fun deleteAll() { items.value = emptyList() }
    override suspend fun getById(id: Long): Transaction? = items.value.firstOrNull { it.id == id }
}

class FakeCategoryRepository : CategoryRepository {
    private val items = MutableStateFlow<List<Category>>(emptyList())
    override fun all(): Flow<List<Category>> = items.asStateFlow()
    override suspend fun upsert(c: Category) {
        items.value = items.value.filterNot { it.name == c.name } + c
    }
    override suspend fun delete(c: Category) { items.value = items.value.filterNot { it.name == c.name } }
    override suspend fun count(): Int = items.value.size
    override suspend fun seedDefaults(list: List<Category>) { items.value = items.value + list }
}

class FakeAccountRepository : AccountRepository {
    private val items = MutableStateFlow<List<Account>>(emptyList())
    private var nextId = 1L
    override fun all(): Flow<List<Account>> = items.asStateFlow()
    override suspend fun getAllOnce(): List<Account> = items.value
    override suspend fun count(): Int = items.value.size
    override suspend fun upsert(a: Account): Long {
        val id = if (a.id == 0L) nextId++ else a.id
        items.value = items.value.filterNot { it.id == id } + a.copy(id = id)
        return id
    }
    override suspend fun seedDefaults(list: List<Account>) {
        items.value = items.value + list.map { it.copy(id = nextId++) }
    }
}

class FakeBudgetRepository : BudgetRepository {
    private val items = MutableStateFlow<List<Budget>>(emptyList())
    override fun all(): Flow<List<Budget>> = items.asStateFlow()
    override suspend fun getByKey(key: String): Budget? = items.value.firstOrNull { it.key == key }
    override suspend fun upsert(b: Budget) { items.value = items.value.filterNot { it.key == b.key } + b }
}
