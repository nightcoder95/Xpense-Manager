# Xpense-Manager Foundation (Phase 0 + Phase 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up a tested, layered foundation (DI, domain UseCases, repositories, new entities + DB migration, SQL-level filtering) and the perf/build tooling — without changing any current screen behavior.

**Architecture:** Hilt for DI; `domain` layer of pure-Kotlin models + repository interfaces + injectable UseCases; `data` layer with Room (entities, DAOs, migration 1→2) and DataStore; existing composables keep working but get their data from new `@HiltViewModel`s backed by UseCases. Filtering moves into SQL. java.time via core-library desugaring.

**Tech Stack:** Kotlin 2.2.10, Compose BOM 2024.09, Room 2.7 (+KSP), Hilt, DataStore, navigation-compose, Robolectric + Roborazzi + Turbine + Room-testing, Macrobenchmark/Baseline Profile.

**Source spec:** `docs/superpowers/specs/2026-06-11-xpense-manager-audit-and-redesign-design.md` (Parts A, B, C4, F).

**Conventions:** This repo is **not** a git repo yet — Task 0 initializes it. Test source set: `app/src/test/java/com/example/...` (Robolectric/JVM). Run unit tests with `./gradlew :app:testDebugUnitTest`. Commit after every task.

---

### Task 0: Initialize git + baseline commit

**Files:**
- Create: `.gitignore` (already exists — verify)

- [ ] **Step 1: Init repo and make the first commit**

```bash
cd /Volumes/work/my_projects/Xpense-Manager
git init
git add -A
git commit -m "chore: baseline snapshot before foundation refactor"
```

- [ ] **Step 2: Create a working branch**

```bash
git checkout -b foundation-phase-0-1
```

Expected: branch `foundation-phase-0-1` created.

---

### Task 1: Add Hilt, nav, DataStore, test libs to the version catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Add versions** under `[versions]`:

```toml
hilt = "2.57"
hiltNavigationCompose = "1.2.0"
turbine = "1.2.0"
roomTesting = "2.7.0"
hiltAndroidTesting = "2.57"
desugar = "2.1.5"
benchmarkMacro = "1.3.4"
baselineprofile = "1.3.4"
```

- [ ] **Step 2: Add libraries** under `[libraries]`:

```toml
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "roomTesting" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hiltAndroidTesting" }
desugar-jdk-libs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugar" }
```

- [ ] **Step 3: Add plugins** under `[plugins]`:

```toml
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 4: Verify catalog parses**

Run: `./gradlew help`
Expected: BUILD SUCCESSFUL (no "unresolved version reference").

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml && git commit -m "build: add hilt, datastore, nav, test deps to catalog"
```

---

### Task 2: Wire Hilt plugin + enable desugaring/minify + enable nav & datastore deps

**Files:**
- Modify: `build.gradle.kts` (root)
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Register the Hilt plugin in root** `build.gradle.kts`

Add to the root `plugins { }` block (alias form, `apply false`):

```kotlin
plugins {
    alias(libs.plugins.hilt) apply false
}
```

(If the root file has no `plugins` block, add one. Keep existing entries.)

- [ ] **Step 2: Apply plugins in** `app/build.gradle.kts` `plugins { }`

```kotlin
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.hilt)
}
```

- [ ] **Step 3: Enable desugaring + schema export + release minify** in `android { }`

In `compileOptions`:

```kotlin
compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
```

In `defaultConfig`, add Room schema export arg:

```kotlin
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
```

In `buildTypes { release { } }` change:

```kotlin
isMinifyEnabled = true
isShrinkResources = true
```

- [ ] **Step 4: Enable deps in** `app/build.gradle.kts` `dependencies { }`

Uncomment / add:

```kotlin
implementation(libs.androidx.navigation.compose)
implementation(libs.androidx.datastore.preferences)
implementation(libs.hilt.android)
implementation(libs.androidx.hilt.navigation.compose)
"ksp"(libs.hilt.compiler)
coreLibraryDesugaring(libs.desugar.jdk.libs)
testImplementation(libs.turbine)
testImplementation(libs.androidx.room.testing)
```

- [ ] **Step 5: Build to verify wiring**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add build.gradle.kts app/build.gradle.kts && git commit -m "build: wire hilt, desugaring, schema export, release minify; enable nav/datastore"
```

---

### Task 3: Application class + Hilt entry points

**Files:**
- Create: `app/src/main/java/com/example/XpenseApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/example/MainActivity.kt:26`

- [ ] **Step 1: Create the Application class**

```kotlin
package com.example

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class XpenseApp : Application()
```

- [ ] **Step 2: Register it + annotate activity**

In `AndroidManifest.xml`, add to `<application>`: `android:name=".XpenseApp"`.

In `MainActivity.kt`, annotate the class:

```kotlin
@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Hilt component generated).

- [ ] **Step 4: Commit**

```bash
git add app/src/main && git commit -m "feat: add @HiltAndroidApp and @AndroidEntryPoint"
```

---

### Task 4: Domain models (pure Kotlin)

**Files:**
- Create: `app/src/main/java/com/example/domain/model/Models.kt`

- [ ] **Step 1: Write the models**

```kotlin
package com.example.domain.model

enum class TxnType { EXPENSE, INCOME, TRANSFER }
enum class AccountType { CASH, BANK, CREDIT_CARD, WALLET }
enum class BudgetPeriod { MONTHLY, ANNUAL }

data class Account(
    val id: Long, val name: String, val type: AccountType,
    val openingBalance: Double, val iconName: String,
    val colorHex: String, val archived: Boolean = false
)

data class Category(
    val name: String, val type: TxnType, val iconName: String,
    val colorHex: String, val sortOrder: Int = 0, val isDefault: Boolean = false
)

data class Transaction(
    val id: Long, val type: TxnType, val amount: Double,
    val category: String, val accountId: Long, val toAccountId: Long? = null,
    val date: Long, val note: String, val tag: String = ""
)

data class Budget(val key: String, val amountLimit: Double, val period: BudgetPeriod)

data class MonthlySummary(val income: Double, val spending: Double) {
    val net: Double get() = income - spending
}

data class CategorySlice(val category: String, val amount: Double, val colorHex: String, val percent: Double)

data class AccountBalance(val account: Account, val balance: Double)

data class SpendingStats(
    val avgPerDay: Double, val avgPerTxn: Double,
    val txnCount: Int, val avgIncomePerTxn: Double
)
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/domain && git commit -m "feat(domain): add pure-kotlin domain models"
```

---

### Task 5: Room entities v2 + mappers

**Files:**
- Create: `app/src/main/java/com/example/data/local/entity/Entities.kt`
- Create: `app/src/main/java/com/example/data/local/Mappers.kt`
- Test: `app/src/test/java/com/example/data/MappersTest.kt`

- [ ] **Step 1: Write the failing mapper test**

```kotlin
package com.example.data

import com.example.data.local.entity.TransactionEntity
import com.example.data.local.toDomain
import com.example.domain.model.TxnType
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {
    @Test fun transactionEntity_mapsToDomain() {
        val e = TransactionEntity(1, "EXPENSE", 100.0, "Food", 2, null, 123L, "n", "t")
        val d = e.toDomain()
        assertEquals(TxnType.EXPENSE, d.type)
        assertEquals(2L, d.accountId)
        assertEquals(100.0, d.amount, 0.0)
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.data.MappersTest"`
Expected: FAIL — unresolved `TransactionEntity`/`toDomain`.

- [ ] **Step 3: Write entities**

```kotlin
package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val type: String, val openingBalance: Double,
    val iconName: String, val colorHex: String, val archived: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("accountId"), Index("category")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, val amount: Double, val category: String,
    val accountId: Long, val toAccountId: Long? = null,
    val date: Long, val note: String, val tag: String = ""
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String, val type: String, val iconName: String,
    val colorHex: String, val sortOrder: Int = 0, val isDefault: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val key: String, val amountLimit: Double, val period: String = "MONTHLY"
)

@Entity(tableName = "scheduled")
data class ScheduledEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, val amount: Double, val category: String,
    val accountId: Long, val recurrence: String, val nextRun: Long,
    val note: String, val enabled: Boolean = true
)
```

- [ ] **Step 4: Write mappers**

```kotlin
package com.example.data.local

import com.example.data.local.entity.*
import com.example.domain.model.*

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
```

- [ ] **Step 5: Run test, verify pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.data.MappersTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/data app/src/test && git commit -m "feat(data): v2 room entities (accounts/scheduled, indices) + domain mappers"
```

---

### Task 6: DAOs with SQL-level filtering

**Files:**
- Create: `app/src/main/java/com/example/data/local/dao/TransactionDao.kt`
- Create: `app/src/main/java/com/example/data/local/dao/CategoryDao.kt`
- Create: `app/src/main/java/com/example/data/local/dao/AccountDao.kt`
- Create: `app/src/main/java/com/example/data/local/dao/BudgetDao.kt`
- Delete: `app/src/main/java/com/example/data/local/TransactionDao.kt` (old monolith)

- [ ] **Step 1: Write the DAOs**

```kotlin
// TransactionDao.kt
package com.example.data.local.dao
import androidx.room.*
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getInRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(t: TransactionEntity): Long
    @Update suspend fun update(t: TransactionEntity)
    @Query("DELETE FROM transactions WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM transactions") suspend fun deleteAll()
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun getById(id: Long): TransactionEntity?
}
```

```kotlin
// CategoryDao.kt
package com.example.data.local.dao
import androidx.room.*
import com.example.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC") fun getAll(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories") suspend fun getAllOnce(): List<CategoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(c: CategoryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(c: List<CategoryEntity>)
    @Delete suspend fun delete(c: CategoryEntity)
    @Query("SELECT COUNT(*) FROM categories") suspend fun count(): Int
}
```

```kotlin
// AccountDao.kt
package com.example.data.local.dao
import androidx.room.*
import com.example.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE archived = 0") fun getAll(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM accounts") suspend fun getAllOnce(): List<AccountEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(a: AccountEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(a: List<AccountEntity>)
    @Query("SELECT COUNT(*) FROM accounts") suspend fun count(): Int
}
```

```kotlin
// BudgetDao.kt
package com.example.data.local.dao
import androidx.room.*
import com.example.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets") fun getAll(): Flow<List<BudgetEntity>>
    @Query("SELECT * FROM budgets WHERE key = :key") suspend fun getByKey(key: String): BudgetEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(b: BudgetEntity)
}
```

- [ ] **Step 2: Delete the old DAO file**

```bash
git rm app/src/main/java/com/example/data/local/TransactionDao.kt
```

- [ ] **Step 3: Build (will fail to link old DB/repo — expected, fixed in Task 7-8)**

Run: `./gradlew :app:compileDebugKotlin` — expect errors only in `AppDatabase.kt`/`FinanceRepository.kt` (old references). Proceed; next tasks fix them.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/data/local/dao && git commit -m "feat(data): split DAOs, add range queries + bulk delete"
```

---

### Task 7: AppDatabase v2 + migration 1→2 + single seed

**Files:**
- Modify: `app/src/main/java/com/example/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/example/data/local/Migrations.kt`
- Create: `app/src/main/java/com/example/data/local/DefaultCategories.kt`
- Test: `app/src/test/java/com/example/data/MigrationTest.kt`

- [ ] **Step 1: Single default-categories source**

```kotlin
package com.example.data.local

import com.example.data.local.entity.CategoryEntity

object DefaultCategories {
    val list: List<CategoryEntity> = listOf(
        CategoryEntity("Others", "EXPENSE", "others", "#9E9E9E", 0, true),
        CategoryEntity("Food and Dining", "EXPENSE", "food", "#FF9800", 1),
        CategoryEntity("Shopping", "EXPENSE", "shopping", "#2196F3", 2),
        CategoryEntity("Travelling", "EXPENSE", "travel", "#9C27B0", 3),
        CategoryEntity("Entertainment", "EXPENSE", "entertainment", "#E91E63", 4),
        CategoryEntity("Medical", "EXPENSE", "medical", "#F44336", 5),
        CategoryEntity("Personal Care", "EXPENSE", "personal_care", "#00BCD4", 6),
        CategoryEntity("Education", "EXPENSE", "education", "#3F51B5", 7),
        CategoryEntity("Bills and Utilities", "EXPENSE", "bills", "#4CAF50", 8),
        CategoryEntity("Investments", "EXPENSE", "investments", "#009688", 9),
        CategoryEntity("Rent", "EXPENSE", "rent", "#795548", 10),
        CategoryEntity("Taxes", "EXPENSE", "taxes", "#607D8B", 11),
        CategoryEntity("Insurance", "EXPENSE", "insurance", "#303F9F", 12),
        CategoryEntity("Gifts and Donation", "EXPENSE", "gifts", "#FF5722", 13),
        CategoryEntity("Salary", "INCOME", "salary", "#4CAF50", 14),
        CategoryEntity("Sold items", "INCOME", "sold_items", "#00BCD4", 15),
        CategoryEntity("Coupons", "INCOME", "coupons", "#E91E63", 16),
        CategoryEntity("Others (Income)", "INCOME", "others", "#9E9E9E", 17, true)
    )
    val accounts = listOf(
        com.example.data.local.entity.AccountEntity(name = "Cash", type = "CASH", openingBalance = 15000.0, iconName = "cash", colorHex = "#43C59E"),
        com.example.data.local.entity.AccountEntity(name = "Bank Account", type = "BANK", openingBalance = 100000.0, iconName = "bank", colorHex = "#6C5DD3"),
        com.example.data.local.entity.AccountEntity(name = "Credit Card", type = "CREDIT_CARD", openingBalance = 0.0, iconName = "card", colorHex = "#F2706B")
    )
}
```

- [ ] **Step 2: Migration 1→2** (`Migrations.kt`)

```kotlin
package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // accounts
        db.execSQL("""CREATE TABLE IF NOT EXISTS accounts (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL,
            type TEXT NOT NULL, openingBalance REAL NOT NULL, iconName TEXT NOT NULL,
            colorHex TEXT NOT NULL, archived INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("INSERT INTO accounts (name,type,openingBalance,iconName,colorHex,archived) VALUES ('Cash','CASH',15000.0,'cash','#43C59E',0)")
        db.execSQL("INSERT INTO accounts (name,type,openingBalance,iconName,colorHex,archived) VALUES ('Bank Account','BANK',100000.0,'bank','#6C5DD3',0)")
        db.execSQL("INSERT INTO accounts (name,type,openingBalance,iconName,colorHex,archived) VALUES ('Credit Card','CREDIT_CARD',0.0,'card','#F2706B',0)")
        // categories: add columns
        db.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE categories ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
        // budgets: add period
        db.execSQL("ALTER TABLE budgets ADD COLUMN period TEXT NOT NULL DEFAULT 'MONTHLY'")
        // transactions: rebuild with accountId/toAccountId, backfill from paymentMode
        db.execSQL("""CREATE TABLE transactions_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL,
            category TEXT NOT NULL, accountId INTEGER NOT NULL, toAccountId INTEGER,
            date INTEGER NOT NULL, note TEXT NOT NULL, tag TEXT NOT NULL DEFAULT '')""")
        db.execSQL("""INSERT INTO transactions_new (id,type,amount,category,accountId,toAccountId,date,note,tag)
            SELECT t.id,t.type,t.amount,t.category,
              COALESCE((SELECT a.id FROM accounts a WHERE a.name = t.paymentMode), 1),
              NULL,t.date,t.note,t.tag FROM transactions t""")
        db.execSQL("DROP TABLE transactions")
        db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions(category)")
        db.execSQL("CREATE TABLE IF NOT EXISTS scheduled (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, accountId INTEGER NOT NULL, recurrence TEXT NOT NULL, nextRun INTEGER NOT NULL, note TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 1)")
    }
}
```

- [ ] **Step 3: Rewrite `AppDatabase.kt`** — v2, all DAOs, single seed via callback using passed `db`, migration registered, Hilt-friendly (no singleton holder).

```kotlin
package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, BudgetEntity::class, AccountEntity::class, ScheduledEntity::class],
    version = 2, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
}
```

(Seeding of a *fresh* install is handled by `SeedDatabaseUseCase` invoked once at app start in Task 12 — removes the double-seed race #1/#2. The Room callback is dropped entirely.)

- [ ] **Step 4: Migration test**

```kotlin
package com.example.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.AppDatabase
import com.example.data.local.MIGRATION_1_2
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java, emptyList(), FrameworkSQLiteOpenHelperFactory()
    )

    @Test fun migrate1To2_backfillsAccountId() {
        helper.createDatabase("migtest", 1).apply {
            execSQL("INSERT INTO categories (name,type,iconName,colorHex) VALUES ('Food','EXPENSE','food','#FF9800')")
            execSQL("INSERT INTO transactions (type,amount,category,paymentMode,date,note,tag) VALUES ('EXPENSE',50.0,'Food','Cash',1000,'n','')")
            close()
        }
        val db = helper.runMigrationsAndValidate("migtest", 2, true, MIGRATION_1_2)
        val c = db.query("SELECT accountId FROM transactions LIMIT 1")
        c.moveToFirst()
        assertTrue(c.getLong(0) >= 1L)
        c.close()
    }
}
```

> Note: schema v1 JSON must exist for `MigrationTestHelper`. Because the project starts at v2, generate the v1 schema by temporarily building at version 1, OR commit a hand-written `app/schemas/com.example.data.local.AppDatabase/1.json` matching the original v1 (transactions with `paymentMode`, categories, budgets). Include that JSON file in this task.

- [ ] **Step 5: Run migration test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.data.MigrationTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/data app/schemas app/src/test && git commit -m "feat(data): AppDatabase v2, migration 1->2 with accountId backfill, single seed source"
```

---

### Task 8: Repository interfaces (domain) + Room impls (data)

**Files:**
- Create: `app/src/main/java/com/example/domain/repository/Repositories.kt`
- Create: `app/src/main/java/com/example/data/repository/RoomRepositories.kt`
- Delete: `app/src/main/java/com/example/data/repository/FinanceRepository.kt`

- [ ] **Step 1: Interfaces**

```kotlin
package com.example.domain.repository
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun all(): Flow<List<Transaction>>
    fun inRange(start: Long, end: Long): Flow<List<Transaction>>
    suspend fun upsert(t: Transaction): Long
    suspend fun delete(id: Long)
    suspend fun deleteAll()
    suspend fun getById(id: Long): Transaction?
}
interface CategoryRepository {
    fun all(): Flow<List<Category>>
    suspend fun upsert(c: Category); suspend fun delete(c: Category); suspend fun count(): Int
    suspend fun seedDefaults(list: List<Category>)
}
interface AccountRepository {
    fun all(): Flow<List<Account>>
    suspend fun getAllOnce(): List<Account>; suspend fun count(): Int
    suspend fun upsert(a: Account): Long; suspend fun seedDefaults(list: List<Account>)
}
interface BudgetRepository {
    fun all(): Flow<List<Budget>>
    suspend fun getByKey(key: String): Budget?; suspend fun upsert(b: Budget)
}
```

- [ ] **Step 2: Impls** (map entity↔domain; `Dispatchers.IO` via injected dispatcher in Task 10 — for now use repository-internal `flowOn`).

```kotlin
package com.example.data.repository
import com.example.data.local.*
import com.example.data.local.dao.*
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class RoomTransactionRepository @Inject constructor(private val dao: TransactionDao) : TransactionRepository {
    override fun all() = dao.getAll().map { it.map { e -> e.toDomain() } }
    override fun inRange(start: Long, end: Long) = dao.getInRange(start, end).map { it.map { e -> e.toDomain() } }
    override suspend fun upsert(t: Transaction) = dao.upsert(t.toEntity())
    override suspend fun delete(id: Long) = dao.deleteById(id)
    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun getById(id: Long) = dao.getById(id)?.toDomain()
}
class RoomCategoryRepository @Inject constructor(private val dao: CategoryDao) : CategoryRepository {
    override fun all() = dao.getAll().map { it.map { e -> e.toDomain() } }
    override suspend fun upsert(c: Category) = dao.upsert(c.toEntity())
    override suspend fun delete(c: Category) = dao.delete(c.toEntity())
    override suspend fun count() = dao.count()
    override suspend fun seedDefaults(list: List<Category>) = dao.upsertAll(list.map { it.toEntity() })
}
class RoomAccountRepository @Inject constructor(private val dao: AccountDao) : AccountRepository {
    override fun all() = dao.getAll().map { it.map { e -> e.toDomain() } }
    override suspend fun getAllOnce() = dao.getAllOnce().map { it.toDomain() }
    override suspend fun count() = dao.count()
    override suspend fun upsert(a: Account) = dao.upsert(a.toEntity())
    override suspend fun seedDefaults(list: List<Account>) = dao.upsertAll(list.map { it.toEntity() })
}
class RoomBudgetRepository @Inject constructor(private val dao: BudgetDao) : BudgetRepository {
    override fun all() = dao.getAll().map { it.map { e -> e.toDomain() } }
    override suspend fun getByKey(key: String) = dao.getByKey(key)?.toDomain()
    override suspend fun upsert(b: Budget) = dao.upsert(b.toEntity())
}
```

(Note: `CategoryRepository.seedDefaults`/`AccountRepository.seedDefaults` accept domain `Category`/`Account`. `DefaultCategories.list` holds entities — add `DefaultCategories.domainCategories`/`domainAccounts` helpers, or have the seed UseCase pass entities through a dedicated repo method. For simplicity, add `fun seedCategoryEntities`/`seedAccountEntities` to the impls used only by the seed UseCase. Keep this consistent in Task 12.)

- [ ] **Step 3: Delete old repository**

```bash
git rm app/src/main/java/com/example/data/repository/FinanceRepository.kt
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:compileDebugKotlin` — will still fail in old screens/VM (Task 13 fixes). Data layer itself compiles.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/domain app/src/main/java/com/example/data && git commit -m "feat(data): repository interfaces + room impls"
```

---

### Task 9: DateRange + Money utilities

**Files:**
- Create: `app/src/main/java/com/example/core/datetime/MonthRange.kt`
- Create: `app/src/main/java/com/example/core/common/CurrencyFormatter.kt`
- Test: `app/src/test/java/com/example/core/MonthRangeTest.kt`
- Test: `app/src/test/java/com/example/core/CurrencyFormatterTest.kt`

- [ ] **Step 1: Failing tests**

```kotlin
// MonthRangeTest.kt
package com.example.core
import com.example.core.datetime.MonthRange
import org.junit.Assert.assertEquals
import org.junit.Test
class MonthRangeTest {
    @Test fun feb2024_has29Days() = assertEquals(29, MonthRange.daysInMonth(2024, 2))
    @Test fun feb2026_has28Days() = assertEquals(28, MonthRange.daysInMonth(2026, 2))
    @Test fun rangeCoversWholeMonth() {
        val (s, e) = MonthRange.bounds(2026, 6)
        assertEquals(true, s < e)
    }
}
```

```kotlin
// CurrencyFormatterTest.kt
package com.example.core
import com.example.core.common.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
class CurrencyFormatterTest {
    private val f = CurrencyFormatter(symbol = "₹")
    @Test fun integerNoDecimals() = assertEquals("₹12,000", f.format(12000.0))
    @Test fun keepsDecimalsWhenPresent() = assertEquals("₹953.5", f.format(953.5))
}
```

- [ ] **Step 2: Run, verify fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.core.*"`
Expected: FAIL (unresolved).

- [ ] **Step 3: Implement**

```kotlin
// MonthRange.kt
package com.example.core.datetime
import java.time.YearMonth
import java.time.ZoneId
object MonthRange {
    fun daysInMonth(year: Int, month: Int) = YearMonth.of(year, month).lengthOfMonth()
    fun bounds(year: Int, month: Int): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val ym = YearMonth.of(year, month)
        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }
}
```

```kotlin
// CurrencyFormatter.kt
package com.example.core.common
import java.util.Locale
class CurrencyFormatter(private val symbol: String = "₹", private val locale: Locale = Locale.US) {
    fun format(amount: Double): String {
        val s = if (amount % 1.0 == 0.0) String.format(locale, "%,d", amount.toLong())
                else String.format(locale, "%,.1f", amount)
        return "$symbol$s"
    }
}
```

- [ ] **Step 4: Run, verify pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.core.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/core app/src/test/java/com/example/core && git commit -m "feat(core): MonthRange (real day counts) + CurrencyFormatter"
```

---

### Task 10: Hilt modules (DB, DAOs, repositories, dispatchers)

**Files:**
- Create: `app/src/main/java/com/example/di/DataModule.kt`
- Create: `app/src/main/java/com/example/core/common/Dispatchers.kt`

- [ ] **Step 1: Dispatcher qualifier**

```kotlin
package com.example.core.common
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Qualifier
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher
data class AppDispatchers(val io: CoroutineDispatcher, val default: CoroutineDispatcher)
```

- [ ] **Step 2: Hilt module**

```kotlin
package com.example.di
import android.content.Context
import androidx.room.Room
import com.example.core.common.AppDispatchers
import com.example.data.local.*
import com.example.data.local.dao.*
import com.example.data.repository.*
import com.example.domain.repository.*
import dagger.Module; import dagger.Provides; import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton fun db(@ApplicationContext c: Context): AppDatabase =
        Room.databaseBuilder(c, AppDatabase::class.java, "expense_manager_db")
            .addMigrations(MIGRATION_1_2).build()
    @Provides fun txnDao(db: AppDatabase) = db.transactionDao()
    @Provides fun catDao(db: AppDatabase) = db.categoryDao()
    @Provides fun accDao(db: AppDatabase) = db.accountDao()
    @Provides fun budDao(db: AppDatabase) = db.budgetDao()
    @Provides @Singleton fun txnRepo(d: TransactionDao): TransactionRepository = RoomTransactionRepository(d)
    @Provides @Singleton fun catRepo(d: CategoryDao): CategoryRepository = RoomCategoryRepository(d)
    @Provides @Singleton fun accRepo(d: AccountDao): AccountRepository = RoomAccountRepository(d)
    @Provides @Singleton fun budRepo(d: BudgetDao): BudgetRepository = RoomBudgetRepository(d)
    @Provides @Singleton fun dispatchers() = AppDispatchers(Dispatchers.IO, Dispatchers.Default)
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:kspDebugKotlin` (or `assembleDebug` once screens compile). Expect Hilt processing OK for `DataModule`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/di app/src/main/java/com/example/core/common/Dispatchers.kt && git commit -m "feat(di): hilt data module + dispatchers"
```

---

### Task 11: UseCases (summary, breakdown, balances, stats, save, search) — TDD

**Files:**
- Create: `app/src/main/java/com/example/domain/usecase/*.kt`
- Test: `app/src/test/java/com/example/domain/UseCasesTest.kt`
- Test fakes: `app/src/test/java/com/example/domain/Fakes.kt`

- [ ] **Step 1: Failing tests** (one file, representative cases — write all)

```kotlin
package com.example.domain
import com.example.domain.model.*
import com.example.domain.usecase.*
import org.junit.Assert.assertEquals
import org.junit.Test

class UseCasesTest {
    private val txns = listOf(
        Transaction(1, TxnType.INCOME, 12000.0, "Salary", 2, null, day(2026,6,7), ""),
        Transaction(2, TxnType.EXPENSE, 685.0, "Bills and Utilities", 1, null, day(2026,6,11), ""),
        Transaction(3, TxnType.EXPENSE, 268.0, "Food and Dining", 1, null, day(2026,6,11), "")
    )
    @Test fun summary_income_spending_net() {
        val s = MonthlySummaryUseCase().from(txns)
        assertEquals(12000.0, s.income, 0.0)
        assertEquals(953.0, s.spending, 0.0)
        assertEquals(11047.0, s.net, 0.0)
    }
    @Test fun stats_perDay_usesRealDayCount() {
        val stats = SpendingStatsUseCase().from(txns, year = 2026, month = 6)
        assertEquals(953.0 / 30.0, stats.avgPerDay, 0.001) // June = 30 days
    }
    @Test fun save_rejectsZeroAmount() {
        val r = SaveTransactionUseCase.validate(amount = 0.0, type = TxnType.EXPENSE, accountId = 1, toAccountId = null)
        assertEquals(false, r.isValid)
    }
    @Test fun save_rejectsTransferToSameAccount() {
        val r = SaveTransactionUseCase.validate(100.0, TxnType.TRANSFER, accountId = 1, toAccountId = 1)
        assertEquals(false, r.isValid)
    }
    @Test fun balances_includeTransfers() {
        val accounts = listOf(
            Account(1,"Cash",AccountType.CASH,1000.0,"cash","#43C59E"),
            Account(2,"Bank",AccountType.BANK,5000.0,"bank","#6C5DD3")
        )
        val t = listOf(Transaction(9, TxnType.TRANSFER, 200.0, "", 1, 2, day(2026,6,1), ""))
        val balances = AccountBalancesUseCase().from(accounts, t).associateBy { it.account.id }
        assertEquals(800.0, balances[1]!!.balance, 0.0)   // cash out
        assertEquals(5200.0, balances[2]!!.balance, 0.0)  // bank in
    }
}
```

(`day(y,m,d)` helper in `Fakes.kt` returns epoch millis at noon; include it.)

- [ ] **Step 2: Run, verify fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.domain.UseCasesTest"`
Expected: FAIL (unresolved use cases).

- [ ] **Step 3: Implement UseCases** (pure functions where possible)

```kotlin
// MonthlySummaryUseCase.kt
package com.example.domain.usecase
import com.example.domain.model.*
class MonthlySummaryUseCase {
    fun from(txns: List<Transaction>) = MonthlySummary(
        income = txns.filter { it.type == TxnType.INCOME }.sumOf { it.amount },
        spending = txns.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }
    )
}
```

```kotlin
// SpendingStatsUseCase.kt
package com.example.domain.usecase
import com.example.core.datetime.MonthRange
import com.example.domain.model.*
class SpendingStatsUseCase {
    fun from(txns: List<Transaction>, year: Int, month: Int): SpendingStats {
        val exp = txns.filter { it.type == TxnType.EXPENSE }
        val inc = txns.filter { it.type == TxnType.INCOME }
        val spend = exp.sumOf { it.amount }
        val days = MonthRange.daysInMonth(year, month)
        return SpendingStats(
            avgPerDay = if (days > 0) spend / days else 0.0,
            avgPerTxn = if (exp.isNotEmpty()) spend / exp.size else 0.0,
            txnCount = exp.size,
            avgIncomePerTxn = if (inc.isNotEmpty()) inc.sumOf { it.amount } / inc.size else 0.0
        )
    }
}
```

```kotlin
// AccountBalancesUseCase.kt
package com.example.domain.usecase
import com.example.domain.model.*
class AccountBalancesUseCase {
    fun from(accounts: List<Account>, txns: List<Transaction>): List<AccountBalance> =
        accounts.map { acc ->
            var bal = acc.openingBalance
            txns.forEach { t ->
                when (t.type) {
                    TxnType.INCOME -> if (t.accountId == acc.id) bal += t.amount
                    TxnType.EXPENSE -> if (t.accountId == acc.id) bal -= t.amount
                    TxnType.TRANSFER -> {
                        if (t.accountId == acc.id) bal -= t.amount
                        if (t.toAccountId == acc.id) bal += t.amount
                    }
                }
            }
            AccountBalance(acc, bal)
        }
}
```

```kotlin
// CategoryBreakdownUseCase.kt
package com.example.domain.usecase
import com.example.domain.model.*
class CategoryBreakdownUseCase {
    fun from(txns: List<Transaction>, categories: List<Category>, type: TxnType): List<CategorySlice> {
        val filtered = txns.filter { it.type == type }
        val total = filtered.sumOf { it.amount }
        return filtered.groupBy { it.category }
            .map { (name, list) ->
                val amt = list.sumOf { it.amount }
                val color = categories.firstOrNull { it.name == name }?.colorHex ?: "#6C5DD3"
                CategorySlice(name, amt, color, if (total > 0) amt / total * 100 else 0.0)
            }.sortedByDescending { it.amount }
    }
}
```

```kotlin
// SaveTransactionUseCase.kt
package com.example.domain.usecase
import com.example.core.common.AppDispatchers
import com.example.domain.model.*
import com.example.domain.repository.TransactionRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ValidationResult(val isValid: Boolean, val error: String? = null)

class SaveTransactionUseCase @Inject constructor(
    private val repo: TransactionRepository, private val dispatchers: AppDispatchers
) {
    suspend operator fun invoke(t: Transaction): Result<Long> {
        val v = validate(t.amount, t.type, t.accountId, t.toAccountId)
        if (!v.isValid) return Result.failure(IllegalArgumentException(v.error))
        return runCatching { withContext(dispatchers.io) { repo.upsert(t) } }
    }
    companion object {
        fun validate(amount: Double, type: TxnType, accountId: Long, toAccountId: Long?): ValidationResult = when {
            amount <= 0.0 -> ValidationResult(false, "Enter an amount greater than 0")
            type == TxnType.TRANSFER && toAccountId == null -> ValidationResult(false, "Choose a destination account")
            type == TxnType.TRANSFER && toAccountId == accountId -> ValidationResult(false, "Transfer accounts must differ")
            else -> ValidationResult(true)
        }
    }
}
```

```kotlin
// SearchTransactionsUseCase.kt
package com.example.domain.usecase
import com.example.domain.model.*
class SearchTransactionsUseCase {
    fun filter(txns: List<Transaction>, query: String, categories: Set<String>, accountIds: Set<Long>): List<Transaction> =
        txns.filter { t ->
            val q = query.isBlank() || listOf(t.note, t.category, t.tag).any { it.contains(query, true) }
            val c = categories.isEmpty() || t.category in categories
            val a = accountIds.isEmpty() || t.accountId in accountIds
            q && c && a
        }
}
```

- [ ] **Step 4: Run, verify pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.domain.UseCasesTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/domain/usecase app/src/test/java/com/example/domain && git commit -m "feat(domain): usecases (summary/stats/balances/breakdown/save/search) + tests"
```

---

### Task 12: Seed UseCase (single path, idempotent) — fixes race #1/#2

**Files:**
- Create: `app/src/main/java/com/example/domain/usecase/SeedDatabaseUseCase.kt`
- Test: `app/src/test/java/com/example/domain/SeedDatabaseUseCaseTest.kt`

- [ ] **Step 1: Failing test** (uses fakes from Task 11 `Fakes.kt`; add `count()` tracking)

```kotlin
package com.example.domain
import com.example.domain.usecase.SeedDatabaseUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SeedDatabaseUseCaseTest {
    @Test fun seedsOnlyWhenEmpty_andIsIdempotent() = runTest {
        val cat = FakeCategoryRepository(); val acc = FakeAccountRepository()
        val uc = SeedDatabaseUseCase(cat, acc)
        uc(); val after1 = cat.count()
        uc(); val after2 = cat.count()
        assertEquals(after1, after2) // second call no-ops
        assertEquals(18, after1)     // 18 default categories
        assertEquals(3, acc.count())
    }
}
```

(Add `FakeCategoryRepository`/`FakeAccountRepository` to `Fakes.kt` if not present, with in-memory lists + `count()`.)

- [ ] **Step 2: Run, verify fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.domain.SeedDatabaseUseCaseTest"`
Expected: FAIL.

- [ ] **Step 3: Implement**

```kotlin
package com.example.domain.usecase
import com.example.data.local.DefaultCategories
import com.example.data.local.toDomain
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.CategoryRepository
import javax.inject.Inject

class SeedDatabaseUseCase @Inject constructor(
    private val categories: CategoryRepository, private val accounts: AccountRepository
) {
    suspend operator fun invoke() {
        if (categories.count() == 0) categories.seedDefaults(DefaultCategories.list.map { it.toDomain() })
        if (accounts.count() == 0) accounts.seedDefaults(DefaultCategories.accounts.map { it.toDomain() })
    }
}
```

- [ ] **Step 4: Run, verify pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.domain.SeedDatabaseUseCaseTest"`
Expected: PASS.

- [ ] **Step 5: Invoke once at startup** — in `MainActivity.onCreate`, inject and call within a lifecycle scope (or a tiny `@HiltViewModel` `AppBootstrapViewModel` whose `init` calls it). Use the ViewModel approach to keep it off the UI thread:

```kotlin
// AppBootstrapViewModel.kt
package com.example.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.usecase.SeedDatabaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppBootstrapViewModel @Inject constructor(seed: SeedDatabaseUseCase) : ViewModel() {
    init { viewModelScope.launch { seed() } }
}
```

Reference it in `MainActivity` (`hiltViewModel<AppBootstrapViewModel>()`) so it's created once.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/domain/usecase/SeedDatabaseUseCase.kt app/src/main/java/com/example/ui/AppBootstrapViewModel.kt app/src/test && git commit -m "feat(domain): single idempotent seed usecase (fixes double-seed race)"
```

---

### Task 13: Rewrite FinanceViewModel onto UseCases (keep current screens working)

**Files:**
- Modify: `app/src/main/java/com/example/ui/FinanceViewModel.kt` (full rewrite, `@HiltViewModel`)
- Modify: `app/src/main/java/com/example/MainActivity.kt:40` (use `hiltViewModel()`)
- Modify: screens only where the old `paymentMode` field name changed → now `accountId`; map account name↔id for display (temporary shim until UI phase).

- [ ] **Step 1: Rewrite the ViewModel** — inject repositories + usecases; expose the same `StateFlow`s the screens already consume (`transactions`, `categories`, `budgets`, `selectedTab`, `viewYear`, `viewMonth`, `isAddSheetOpen`, `editingTransaction`, `searchQuery`, filters) so existing composables compile with minimal edits. Replace direct DB construction; route writes through usecases; surface errors via a `SharedFlow<String>`.

```kotlin
@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val txnRepo: TransactionRepository,
    private val catRepo: CategoryRepository,
    private val budgetRepo: BudgetRepository,
    private val accountRepo: AccountRepository,
    private val saveTransaction: SaveTransactionUseCase,
    private val dispatchers: AppDispatchers
) : ViewModel() {
    val transactions = txnRepo.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = catRepo.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = budgetRepo.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accounts = accountRepo.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _errors = MutableSharedFlow<String>(); val errors = _errors.asSharedFlow()
    // ... selectedTab/view month/add sheet/filter state as before ...
    fun save(t: Transaction) = viewModelScope.launch {
        saveTransaction(t).onFailure { _errors.emit(it.message ?: "Could not save") }.onSuccess { closeAddTransaction() }
    }
    fun deleteTransaction(id: Long) = viewModelScope.launch { runCatching { txnRepo.delete(id) }.onFailure { _errors.emit("Delete failed") } }
    fun clearAllData() = viewModelScope.launch { txnRepo.deleteAll() } // bulk, transactional
}
```

(Keep method names the screens already call: `selectTab`, `nextMonth`, `prevMonth`, `openAddTransaction`, `closeAddTransaction`, `updateSearchQuery`, `setFilterCategory`, `setFilterPaymentMode`, `saveBudget`, `createCategory`, `deleteCategory`, `seedDemoData`. Re-implement `saveTransaction(...)` signature used by `AddTransactionSheet` to build a `Transaction` (map account name→id) and call `save`.)

- [ ] **Step 2: Update screens for the `paymentMode`→`accountId` rename** — in `AddTransactionSheet`, `HomeTab`, `TransactionsTab`, map selected account name to `accountId` via the `accounts` list; display `account.name`. Minimal shim; full UI redo is Phase 4.

- [ ] **Step 3: `MainActivity` uses `hiltViewModel()`**

```kotlin
val viewModel: FinanceViewModel = androidx.hilt.navigation.compose.hiltViewModel()
```

- [ ] **Step 4: Build + run unit tests + assemble**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL; all tests green.

- [ ] **Step 5: Commit**

```bash
git add app/src && git commit -m "refactor(ui): FinanceViewModel on hilt+usecases; bulk clear; error surface; account shim"
```

---

### Task 14: Macrobenchmark module + Baseline Profile + StrictMode

**Files:**
- Create: `benchmark/` module (`benchmark/build.gradle.kts`, `benchmark/src/main/java/com/example/benchmark/StartupBenchmark.kt`)
- Modify: `settings.gradle.kts` (`include(":benchmark")`)
- Modify: `app/src/debug/java/com/example/DebugStrictMode.kt` (init in `XpenseApp` debug only)

- [ ] **Step 1: Add benchmark module + Baseline Profile plugin** (use `androidx.benchmark:benchmark-macro-junit4` + `androidx.baselineprofile`). Minimal `StartupBenchmark` measuring `CompilationMode.None()` vs `Partial(BaselineProfile)` cold start of `com.aistudio.expensemanager.kxmpzq`.

```kotlin
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()
    @Test fun startupCold() = rule.measureRepeated(
        packageName = "com.aistudio.expensemanager.kxmpzq",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5, startupMode = StartupMode.COLD
    ) { pressHome(); startActivityAndWait() }
}
```

- [ ] **Step 2: StrictMode in debug**

```kotlin
// app/src/debug/.../DebugStrictMode.kt
fun installStrictMode() {
    StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyLog().build())
}
```

Call from `XpenseApp.onCreate()` guarded by `BuildConfig.DEBUG`.

- [ ] **Step 3: Verify benchmark compiles**

Run: `./gradlew :benchmark:assemble`
Expected: BUILD SUCCESSFUL. (Running it needs a device/emulator — document, don't block.)

- [ ] **Step 4: Commit**

```bash
git add benchmark settings.gradle.kts app/src/debug && git commit -m "build: macrobenchmark module, baseline profile scaffold, debug StrictMode"
```

---

### Task 15: Full regression + phase wrap

- [ ] **Step 1: Run everything**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL; all unit tests pass.

- [ ] **Step 2: Manual smoke** (emulator): launch → categories+accounts seeded once → add a transaction (zero amount blocked, valid saves) → switch tabs → clear data works. No crash on rotation mid-add.

- [ ] **Step 3: Commit + tag**

```bash
git add -A && git commit -m "chore: phase 0-1 foundation complete" && git tag phase-0-1-done
```

---

## Self-Review

**Spec coverage (Parts A/B/C4/F):**
- A1 #1/#2 double-seed race → Task 7 (drop callback) + Task 12 (single idempotent seed). ✅
- A1 #3 rotation loss → partially (VM-backed save shim Task 13); full `rememberSaveable`/Add-VM is **Phase 2/Phase 4** — noted, out of this plan's scope. ⚠ (deferred, called out)
- A1 #4/#5 validation + error surface → Task 11 (validate) + Task 13 (`errors` flow). ✅
- A1 #6 bulk delete → Task 6 `deleteAll` + Task 13. ✅
- A1 #7/#8 transfers + real accounts → Task 5 entities, Task 11 `AccountBalancesUseCase`, Task 7 seed accounts. ✅
- A2 #9 day-count → Task 9/Task 11 stats. ✅  A2 #10/#11 formatting/currency → Task 9. ✅
- A3 logic-in-composables / god VM / no DI → Tasks 10–13. ✅
- B SQL filtering + indices → Task 5/Task 6. ✅  Migration → Task 7. ✅
- C4 perf/build (minify, desugaring, baseline, StrictMode) → Task 2, Task 14. ✅
- F unit/migration/macrobenchmark tests → Tasks 5–12, 14. ✅
- **Deferred to later plans (intentional):** #12 filter UI, #13 today/yesterday labels, #14 capitalize, #15 donut dp, #17 insets/bottom-sheet, all UI screens (Phases 2–7).

**Placeholder scan:** No "TODO/TBD" in steps; the v1 schema JSON in Task 7 Step 4 is the one manual artifact — explicitly described.

**Type consistency:** `TxnType`/`AccountType` enums used consistently; `toDomain()/toEntity()` names consistent; repo method names (`upsert`, `deleteAll`, `inRange`, `seedDefaults`, `count`) match between interface (Task 8) and DAOs (Task 6) and usages (Tasks 11–13). `SaveTransactionUseCase.validate` signature identical in test (Task 11) and impl. `DefaultCategories.list` is 18 categories → matches `SeedDatabaseUseCaseTest` assertion (Task 12).

**Known follow-on plans:** Phase 2 (remaining bug fixes incl. rotation/Add-VM), Phase 3 (design system + nav), Phase 4 (core screens pixel-match), Phase 5 (secondary screens), Phase 6 (premium placeholders), Phase 7 (a11y/polish).
