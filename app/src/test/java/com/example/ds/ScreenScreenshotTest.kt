package com.example.ds

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.domain.model.Account
import com.example.domain.model.AccountBalance
import com.example.domain.model.AccountType
import com.example.domain.model.Budget
import com.example.domain.model.Category
import com.example.domain.model.CategorySlice
import com.example.domain.model.MonthlySummary
import com.example.domain.model.SpendingStats
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType
import com.example.ui.feature.accounts.AccountsContent
import com.example.ui.feature.accounts.AccountsUiState
import com.example.ui.feature.analysis.AnalysisContent
import com.example.ui.feature.analysis.AnalysisUiState
import com.example.ui.feature.home.HomeContent
import com.example.ui.feature.home.HomeUiState
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class ScreenScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun home_matches() {
        val state = HomeUiState(
            greeting = "Good Morning",
            summary = MonthlySummary(income = 12000.0, spending = 953.0),
            budget = null,
            recent = listOf(
                Transaction(1, TxnType.EXPENSE, 685.0, "Bills and Utilities", 1, null, 0, ""),
                Transaction(2, TxnType.EXPENSE, 250.0, "Food and Dining", 1, null, 0, "")
            ),
            setupDone = 2,
            categories = listOf(
                Category("Bills and Utilities", TxnType.EXPENSE, "bills", "#F2706B"),
                Category("Food and Dining", TxnType.EXPENSE, "food", "#E3B341")
            ),
            accountNames = mapOf(1L to "Cash")
        )
        compose.setContent { MyApplicationTheme { HomeContent(state) } }
        compose.onRoot().captureRoboImage("build/roborazzi/home.png")
    }

    @Test fun analysis_matches() {
        val state = AnalysisUiState(
            rangeTitle = "June 2026",
            txnCount = 4,
            summary = MonthlySummary(12000.0, 953.0),
            categorySpending = listOf(
                CategorySlice("Bills and Utilities", 685.0, "#F2706B", 71.9),
                CategorySlice("Food and Dining", 268.0, "#E3B341", 28.1)
            ),
            paymentSpending = listOf(CategorySlice("Cash", 953.0, "#43C59E", 100.0)),
            stats = SpendingStats(avgPerDay = 86.64, avgPerTxn = 317.7, txnCount = 3, avgIncomePerTxn = 12000.0)
        )
        compose.setContent { MyApplicationTheme { AnalysisContent(state) } }
        compose.onRoot().captureRoboImage("build/roborazzi/analysis.png")
    }

    @Test fun accounts_matches() {
        val state = AccountsUiState(
            balances = listOf(
                AccountBalance(Account(1, "Cash", AccountType.CASH, 15000.0, "wallet", "#43C59E"), 14047.0),
                AccountBalance(Account(2, "Bank Account", AccountType.BANK, 100000.0, "bank", "#6C5DD3"), 112000.0)
            ),
            totalNet = 126047.0
        )
        compose.setContent { MyApplicationTheme { AccountsContent(state) } }
        compose.onRoot().captureRoboImage("build/roborazzi/accounts.png")
    }
}
