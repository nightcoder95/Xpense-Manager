package com.example.data.repository

import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.domain.model.Account
import com.example.domain.model.Budget
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.BudgetRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {
    override fun all() = dao.getAll().map { list -> list.map { it.toDomain() } }
    override fun inRange(start: Long, end: Long) = dao.getInRange(start, end).map { list -> list.map { it.toDomain() } }
    override suspend fun upsert(t: Transaction) = dao.upsert(t.toEntity())
    override suspend fun update(t: Transaction) = dao.update(t.toEntity())
    override suspend fun delete(id: Long) = dao.deleteById(id)
    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun getById(id: Long) = dao.getById(id)?.toDomain()
    override suspend fun countByAccount(accountId: Long) = dao.countByAccount(accountId)
}

class RoomCategoryRepository @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {
    override fun all() = dao.getAll().map { list -> list.map { it.toDomain() } }
    override suspend fun upsert(c: Category) = dao.upsert(c.toEntity())
    override suspend fun delete(c: Category) = dao.delete(c.toEntity())
    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun count() = dao.count()
    override suspend fun seedDefaults(list: List<Category>) = dao.upsertAll(list.map { it.toEntity() })
}

class RoomAccountRepository @Inject constructor(
    private val dao: AccountDao
) : AccountRepository {
    override fun all() = dao.getAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getAllOnce() = dao.getAllOnce().map { it.toDomain() }
    override suspend fun getById(id: Long) = dao.getById(id)?.toDomain()
    override suspend fun count() = dao.count()
    override suspend fun upsert(a: Account) = dao.upsert(a.toEntity())
    override suspend fun delete(id: Long) = dao.deleteById(id)
    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun seedDefaults(list: List<Account>) = dao.upsertAll(list.map { it.toEntity() })
}

class RoomBudgetRepository @Inject constructor(
    private val dao: BudgetDao
) : BudgetRepository {
    override fun all() = dao.getAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getByKey(key: String) = dao.getByKey(key)?.toDomain()
    override suspend fun upsert(b: Budget) = dao.upsert(b.toEntity())
    override suspend fun delete(key: String) = dao.deleteByKey(key)
    override suspend fun deleteAll() = dao.deleteAll()
}
