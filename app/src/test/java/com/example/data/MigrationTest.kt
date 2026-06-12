package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migtest.db"

    private fun openV1(): SupportSQLiteOpenHelper {
        context.deleteDatabase(dbName)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `paymentMode` TEXT NOT NULL, `date` INTEGER NOT NULL, `note` TEXT NOT NULL, `tag` TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`name` TEXT NOT NULL, `type` TEXT NOT NULL, `iconName` TEXT NOT NULL, `colorHex` TEXT NOT NULL, PRIMARY KEY(`name`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`month` TEXT NOT NULL, `amountLimit` REAL NOT NULL, PRIMARY KEY(`month`))")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config)
    }

    @Test fun migrate1To2_backfillsAccountId_renamesBudgetKey_seedsAccounts() {
        // --- seed a v1 database ---
        openV1().use { helper ->
            helper.writableDatabase.apply {
                execSQL("INSERT INTO categories (name,type,iconName,colorHex) VALUES ('Food','EXPENSE','food','#FF9800')")
                execSQL("INSERT INTO transactions (type,amount,category,paymentMode,date,note,tag) VALUES ('EXPENSE',50.0,'Food','Bank Account',1000,'n','')")
                execSQL("INSERT INTO budgets (month,amountLimit) VALUES ('2026-06',5000.0)")
            }
        }

        // --- run the migration manually against the v1 file ---
        openV1Existing().use { helper ->
            val db = helper.writableDatabase
            MIGRATION_1_2.migrate(db)
            db.version = 2 // mark migrated so Room validates instead of re-running the migration

            db.query("SELECT accountId FROM transactions LIMIT 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(2L, c.getLong(0)) // backfilled to seeded "Bank Account" (id 2)
            }
            db.query("SELECT key, period FROM budgets LIMIT 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("2026-06", c.getString(0))
                assertEquals("MONTHLY", c.getString(1))
            }
            db.query("SELECT COUNT(*) FROM accounts").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(3, c.getInt(0))
            }
        }

        // --- open through Room v2: validates the migrated schema matches the entities ---
        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2)
            .build()
        room.openHelper.writableDatabase // forces open + Room schema validation
        room.close()
    }

    /** Reopen the existing v1 file without recreating it. */
    private fun openV1Existing(): SupportSQLiteOpenHelper {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config)
    }
}
