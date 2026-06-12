package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE key = :key")
    suspend fun getByKey(key: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(b: BudgetEntity)

    @Query("DELETE FROM budgets WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
