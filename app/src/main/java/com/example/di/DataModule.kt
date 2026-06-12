package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.core.common.AppDispatchers
import com.example.data.local.AppDatabase
import com.example.data.local.MIGRATION_1_2
import com.example.data.local.MIGRATION_2_3
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.repository.RoomAccountRepository
import com.example.data.repository.RoomBudgetRepository
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.BudgetRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun db(@ApplicationContext c: Context): AppDatabase =
        Room.databaseBuilder(c, AppDatabase::class.java, "expense_manager_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides fun txnDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun catDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun accDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun budDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides @Singleton
    fun txnRepo(d: TransactionDao): TransactionRepository = RoomTransactionRepository(d)
    @Provides @Singleton
    fun catRepo(d: CategoryDao): CategoryRepository = RoomCategoryRepository(d)
    @Provides @Singleton
    fun accRepo(d: AccountDao): AccountRepository = RoomAccountRepository(d)
    @Provides @Singleton
    fun budRepo(d: BudgetDao): BudgetRepository = RoomBudgetRepository(d)

    @Provides @Singleton
    fun dispatchers(): AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default)

    @Provides @Singleton
    fun prefsRepo(@ApplicationContext c: Context): com.example.domain.repository.PreferencesRepository =
        com.example.data.preferences.DataStorePreferencesRepository(c)
}
