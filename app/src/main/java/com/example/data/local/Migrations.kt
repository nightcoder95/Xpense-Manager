package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- accounts (new) + seed defaults ---
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL,
                type TEXT NOT NULL, openingBalance REAL NOT NULL, iconName TEXT NOT NULL,
                colorHex TEXT NOT NULL, archived INTEGER NOT NULL DEFAULT 0)"""
        )
        db.execSQL("INSERT INTO accounts (name,type,openingBalance,iconName,colorHex,archived) VALUES ('Cash','CASH',15000.0,'cash','#43C59E',0)")
        db.execSQL("INSERT INTO accounts (name,type,openingBalance,iconName,colorHex,archived) VALUES ('Bank Account','BANK',100000.0,'bank','#6C5DD3',0)")
        db.execSQL("INSERT INTO accounts (name,type,openingBalance,iconName,colorHex,archived) VALUES ('Credit Card','CREDIT_CARD',0.0,'card','#F2706B',0)")

        // --- categories: add sortOrder + isDefault ---
        db.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE categories ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")

        // --- budgets: rebuild month -> key, add period ---
        db.execSQL(
            """CREATE TABLE budgets_new (
                key TEXT PRIMARY KEY NOT NULL, amountLimit REAL NOT NULL,
                period TEXT NOT NULL DEFAULT 'MONTHLY')"""
        )
        db.execSQL("INSERT INTO budgets_new (key,amountLimit,period) SELECT month, amountLimit, 'MONTHLY' FROM budgets")
        db.execSQL("DROP TABLE budgets")
        db.execSQL("ALTER TABLE budgets_new RENAME TO budgets")

        // --- transactions: rebuild with accountId/toAccountId, backfill from paymentMode ---
        db.execSQL(
            """CREATE TABLE transactions_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL,
                category TEXT NOT NULL, accountId INTEGER NOT NULL, toAccountId INTEGER,
                date INTEGER NOT NULL, note TEXT NOT NULL, tag TEXT NOT NULL DEFAULT '')"""
        )
        db.execSQL(
            """INSERT INTO transactions_new (id,type,amount,category,accountId,toAccountId,date,note,tag)
                SELECT t.id,t.type,t.amount,t.category,
                  COALESCE((SELECT a.id FROM accounts a WHERE a.name = t.paymentMode), 1),
                  NULL,t.date,t.note,t.tag FROM transactions t"""
        )
        db.execSQL("DROP TABLE transactions")
        db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions(category)")

        // --- scheduled (new) ---
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS scheduled (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL,
                category TEXT NOT NULL, accountId INTEGER NOT NULL, recurrence TEXT NOT NULL,
                nextRun INTEGER NOT NULL, note TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 1)"""
        )
    }
}

// Scheduled-transactions feature was removed; drop its now-unused table.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS scheduled")
    }
}
