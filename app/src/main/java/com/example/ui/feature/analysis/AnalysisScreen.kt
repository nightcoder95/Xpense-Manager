package com.example.ui.feature.analysis

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
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.common.CurrencyFormatter
import com.example.core.designsystem.DateRangeHeader
import com.example.core.designsystem.MonthSelector
import com.example.core.designsystem.SectionHeader
import com.example.core.designsystem.SummaryCard
import com.example.core.designsystem.XColors
import com.example.core.designsystem.XPillToggle
import com.example.core.designsystem.XSurfaceCard
import com.example.core.designsystem.toComposeColor
import com.example.domain.model.CategorySlice
import com.example.ui.components.DonutChart
import com.example.ui.components.Portion

private val currency = CurrencyFormatter("₹")

@Composable
fun AnalysisScreen(
    onSetBudget: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AnalysisContent(
        state = state,
        onPeriod = viewModel::setPeriod,
        onPrev = viewModel::prev,
        onNext = viewModel::next,
        onSetBudget = onSetBudget,
        modifier = modifier
    )
}

@Composable
fun AnalysisContent(
    state: AnalysisUiState,
    onPeriod: (AnalysisPeriod) -> Unit = {},
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    onSetBudget: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var catMode by remember { mutableStateOf("Spending") }
    var payMode by remember { mutableStateOf("Spending") }

    LazyColumn(
        modifier = modifier.fillMaxWidth().background(XColors.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Text("Analysis", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = XColors.TextPrimary) }
        item {
            XPillToggle(
                options = listOf("Week", "Month", "Year", "Custom"),
                selected = state.period.name.lowercase().replaceFirstChar { it.uppercase() },
                onSelect = { onPeriod(AnalysisPeriod.valueOf(it.uppercase())) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            XSurfaceCard {
                MonthSelector(label = state.rangeTitle, onPrev = onPrev, onNext = onNext)
                Spacer(Modifier.height(4.dp))
                SectionHeader("${state.txnCount} TRANSACTIONS", modifier = Modifier.fillMaxWidth())
            }
        }
        item { SummaryCard(spending = state.summary.spending, income = state.summary.income) }
        item {
            Text("Budget", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
        }
        item { BudgetCard(state, onSetBudget) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
                Spacer(Modifier.size(6.dp))
                Icon(Icons.Filled.Diamond, null, tint = XColors.AccentGold, modifier = Modifier.size(16.dp))
            }
        }
        item { TrendsUpsell() }
        item { Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary) }
        item {
            val slices = if (catMode == "Spending") state.categorySpending else state.categoryIncome
            DonutCard(slices, catMode, listOf("Spending", "Income")) { catMode = it }
        }
        item { Text("Payment modes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary) }
        item {
            val slices = when (payMode) {
                "Income" -> state.paymentIncome
                "Transfers" -> state.paymentTransfer
                else -> state.paymentSpending
            }
            XSurfaceCard {
                XPillToggle(listOf("Spending", "Income", "Transfers"), payMode, onSelect = { payMode = it }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                if (slices.isEmpty()) Text("No data", style = MaterialTheme.typography.bodyMedium, color = XColors.TextSecondary)
                else slices.forEach { SliceRow(it) }
            }
        }
        item { Text("Stats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary) }
        item { StatsCard(state) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun BudgetCard(state: AnalysisUiState, onSetBudget: () -> Unit) {
    XSurfaceCard {
        if (state.budget == null) {
            Text("No Budget for This Month?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Setting a budget for your spending is a crucial step in achieving your financial goals.", style = MaterialTheme.typography.bodyMedium, color = XColors.TextSecondary)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(XColors.SurfaceVariant).clickable { onSetBudget() }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Set Up Budget", color = XColors.TextPrimary, fontWeight = FontWeight.Bold)
            }
        } else {
            val pct = if (state.budget.amountLimit > 0) (state.summary.spending / state.budget.amountLimit).coerceIn(0.0, 1.0).toFloat() else 0f
            Text("${currency.format(state.summary.spending)} of ${currency.format(state.budget.amountLimit)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(XColors.SurfaceVariant)) {
                Box(Modifier.fillMaxWidth(pct).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (pct >= 1f) XColors.Spending else XColors.Income))
            }
        }
    }
}

@Composable
private fun TrendsUpsell() {
    XSurfaceCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Discover Financial Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Gain insights into your spending patterns with trend analysis in Premium.", style = MaterialTheme.typography.bodyMedium, color = XColors.TextSecondary)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(XColors.AccentGold.copy(alpha = 0.18f)).padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Upgrade Now", color = XColors.AccentGold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DonutCard(slices: List<CategorySlice>, mode: String, options: List<String>, onMode: (String) -> Unit) {
    XSurfaceCard {
        XPillToggle(options, mode, onSelect = onMode, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DonutChart(
                portions = slices.map { Portion(it.category, it.amount, it.colorHex.toComposeColor()) },
                modifier = Modifier.size(180.dp)
            ) {
                val top = slices.firstOrNull()
                if (top != null) Text("${"%.1f".format(top.percent)}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
            }
        }
        Spacer(Modifier.height(16.dp))
        slices.take(5).forEach { SliceRow(it) }
    }
}

@Composable
private fun SliceRow(slice: CategorySlice) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(slice.colorHex.toComposeColor().copy(alpha = 0.2f)))
        Spacer(Modifier.size(12.dp))
        Text(slice.category, style = MaterialTheme.typography.titleMedium, color = XColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(currency.format(slice.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
    }
}

@Composable
private fun StatsCard(state: AnalysisUiState) {
    XSurfaceCard {
        SectionHeader("Average Spending")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCell("Per day", currency.format(state.stats.avgPerDay), Modifier.weight(1f))
            StatCell("Per transaction", currency.format(state.stats.avgPerTxn), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        SectionHeader("Average Income")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCell("Per day", currency.format(state.summary.income / 30.0), Modifier.weight(1f))
            StatCell("Per transaction", currency.format(state.stats.avgIncomePerTxn), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = XColors.TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = XColors.TextPrimary)
    }
}
