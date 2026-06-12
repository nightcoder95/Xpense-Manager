package com.example.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.designsystem.SummaryCard
import com.example.core.designsystem.TransactionRow
import com.example.core.designsystem.XColors
import com.example.core.designsystem.XPillToggle
import com.example.core.designsystem.XSurfaceCard
import com.example.core.designsystem.toComposeColor
import com.example.ui.util.CategoryIconHelper
import com.example.ui.util.DateTimeUtils

@Composable
fun HomeScreen(
    onSeeAllRecent: () -> Unit,
    onSetBudget: () -> Unit,
    onCustomize: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state, onSeeAllRecent, onSetBudget, onCustomize, onTransactionClick, modifier)
}

/** Stateless content — directly screenshot-testable with a fixed [HomeUiState]. */
@Composable
fun HomeContent(
    state: HomeUiState,
    onSeeAllRecent: () -> Unit = {},
    onSetBudget: () -> Unit = {},
    onCustomize: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(XColors.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Header(state.greeting) }
        item {
            SummaryCard(
                spending = state.summary.spending,
                income = state.summary.income
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Recent Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
                Pill("See all", onSeeAllRecent)
            }
        }
        item { RecentCard(state, onTransactionClick) }
        item {
            Text("Budgets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
        }
        item { BudgetsCard(hasBudget = state.budget != null, limit = state.budget?.amountLimit ?: 0.0, spent = state.summary.spending, onSetBudget = onSetBudget) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Pill("☰  Customize", onCustomize)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Header(greeting: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("$greeting,", style = MaterialTheme.typography.bodyMedium, color = XColors.TextSecondary)
            Text("Guest User", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = XColors.TextPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIcon(Icons.Filled.Search, "Search", XColors.SurfaceVariant, XColors.TextPrimary)
            CircleIcon(Icons.Filled.Person, "Profile", XColors.SurfaceVariant, XColors.TextSecondary)
        }
    }
}

@Composable
private fun CircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, bg: androidx.compose.ui.graphics.Color, tint: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Pill(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(16.dp)).background(XColors.SurfaceVariant).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 8.dp)
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = XColors.TextPrimary, fontWeight = FontWeight.Bold) }
}

@Composable
private fun RecentCard(state: HomeUiState, onClick: (Long) -> Unit) {
    XSurfaceCard(Modifier.padding(top = 0.dp)) {
        if (state.recent.isEmpty()) {
            Text("No transactions this month", style = MaterialTheme.typography.bodyMedium, color = XColors.TextSecondary, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            state.recent.forEach { t ->
                val cat = state.categories.firstOrNull { it.name == t.category }
                key(t.id) {
                    TransactionRow(
                        category = t.category,
                        account = state.accountNames[t.accountId] ?: "",
                        note = t.note,
                        amount = t.amount,
                        type = t.type.name,
                        time = if (DateTimeUtils.isToday(t.date)) "Today" else DateTimeUtils.formatDate(t.date),
                        color = (cat?.colorHex ?: "#6C5DD3").toComposeColor(),
                        icon = CategoryIconHelper.getCategoryIcon(cat?.iconName ?: "others"),
                        onClick = { onClick(t.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetsCard(hasBudget: Boolean, limit: Double, spent: Double, onSetBudget: () -> Unit) {
    XSurfaceCard {
        XPillToggle(options = listOf("Monthly", "Annual"), selected = "Monthly", onSelect = {}, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        if (!hasBudget) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(XColors.SurfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Savings, null, tint = XColors.TextSecondary, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("No Budget Yet?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Set a monthly budget to achieve your saving goals.", style = MaterialTheme.typography.bodyMedium, color = XColors.TextSecondary)
                Spacer(Modifier.height(16.dp))
                Pill("Set Budget", onSetBudget)
            }
        } else {
            val pct = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0).toFloat() else 0f
            Text("₹${spent.toLong()} of ₹${limit.toLong()}", style = MaterialTheme.typography.titleMedium, color = XColors.TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(XColors.SurfaceVariant)) {
                Box(Modifier.fillMaxWidth(pct).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (pct >= 1f) XColors.Spending else XColors.Income))
            }
        }
    }
}
