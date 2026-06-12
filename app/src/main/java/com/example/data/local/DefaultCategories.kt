package com.example.data.local

import com.example.data.local.entity.AccountEntity
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

    // Single empty account so a fresh install is usable (Add Transaction needs an account)
    // without seeding any fake balances. Users add their own real accounts.
    val accounts: List<AccountEntity> = listOf(
        AccountEntity(name = "Cash", type = "CASH", openingBalance = 0.0, iconName = "cash", colorHex = "#43C59E")
    )
}
