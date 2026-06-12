package com.example.ds

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.core.designsystem.BottomNavBar
import com.example.core.designsystem.TransactionRow
import com.example.core.designsystem.XColors
import com.example.core.designsystem.XPillToggle
import com.example.core.designsystem.XSurfaceCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun surfaceCard_matches() {
        compose.setContent { MyApplicationTheme { XSurfaceCard { Text("Hello") } } }
        compose.onRoot().captureRoboImage("build/roborazzi/surfaceCard.png")
    }

    @Test fun pillToggle_matches() {
        compose.setContent {
            MyApplicationTheme {
                XPillToggle(
                    options = listOf("Expense", "Income", "Transfer"),
                    selected = "Expense",
                    onSelect = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        compose.onRoot().captureRoboImage("build/roborazzi/pillToggle.png")
    }

    @Test fun transactionRow_matches() {
        compose.setContent {
            MyApplicationTheme {
                TransactionRow(
                    category = "Food and Dining",
                    account = "Credit Card",
                    note = "Lunch",
                    amount = 1250.0,
                    type = "EXPENSE",
                    time = "12:30 PM",
                    color = XColors.Spending,
                    icon = Icons.Filled.Fastfood,
                    onClick = {}
                )
            }
        }
        compose.onRoot().captureRoboImage("build/roborazzi/transactionRow.png")
    }

    @Test fun bottomNavBar_matches() {
        compose.setContent {
            MyApplicationTheme {
                BottomNavBar(current = "home", onSelect = {}, onAdd = {})
            }
        }
        compose.onRoot().captureRoboImage("build/roborazzi/bottomNavBar.png")
    }
}
