package com.example.ui.feature.budgets

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.common.CurrencyFormatter
import com.example.core.designsystem.XColors
import com.example.core.designsystem.XPillToggle
import com.example.core.designsystem.XPrimaryButton
import com.example.core.designsystem.XSurfaceCard
import com.example.core.designsystem.toComposeColor
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.CategorySlice

private val currency = CurrencyFormatter("₹")

@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dialogOpen by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.messages.collect { msg -> android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show() }
    }

    LazyColumn(modifier.fillMaxWidth().background(XColors.Background), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(XColors.SurfaceVariant).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = XColors.TextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text("Budgets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
            }
        }
        item {
            XPillToggle(
                options = listOf("Monthly", "Annual"),
                selected = if (state.period == BudgetPeriod.ANNUAL) "Annual" else "Monthly",
                onSelect = { viewModel.setPeriod(if (it == "Annual") BudgetPeriod.ANNUAL else BudgetPeriod.MONTHLY) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            XSurfaceCard {
                if (state.limit == null) {
                    Text("No budget set", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Spent ${currency.format(state.spent)} this period.", style = MaterialTheme.typography.bodyMedium, color = XColors.TextSecondary)
                } else {
                    val pct = (state.spent / state.limit!!).coerceIn(0.0, 1.0).toFloat()
                    Text("${currency.format(state.spent)} of ${currency.format(state.limit!!)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = XColors.TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    ProgressBar(pct)
                }
                Spacer(Modifier.height(16.dp))
                XPrimaryButton(if (state.limit == null) "Set Budget" else "Edit Budget", onClick = { dialogOpen = true })
                if (state.limit != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Remove budget",
                        style = MaterialTheme.typography.titleMedium, color = XColors.Spending, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.deleteBudget() }.padding(vertical = 10.dp)
                    )
                }
            }
        }
        item { Text("By category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary) }
        items(state.categories, key = { it.category }) { slice -> CategoryBudgetRow(slice, state.spent) }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (dialogOpen) {
        BudgetDialog(initial = state.limit, onConfirm = { viewModel.saveBudget(it); dialogOpen = false }, onDismiss = { dialogOpen = false })
    }
}

@Composable
private fun ProgressBar(pct: Float) {
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(XColors.SurfaceVariant)) {
        Box(Modifier.fillMaxWidth(pct).height(10.dp).clip(RoundedCornerShape(5.dp)).background(if (pct >= 1f) XColors.Spending else XColors.Income))
    }
}

@Composable
private fun CategoryBudgetRow(slice: CategorySlice, total: Double) {
    XSurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(slice.category, style = MaterialTheme.typography.titleMedium, color = XColors.TextPrimary)
            Text(currency.format(slice.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        val pct = if (total > 0) (slice.amount / total).toFloat() else 0f
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(XColors.SurfaceVariant)) {
            Box(Modifier.fillMaxWidth(pct).height(8.dp).clip(RoundedCornerShape(4.dp)).background(slice.colorHex.toComposeColor()))
        }
    }
}

@Composable
private fun BudgetDialog(initial: Double?, onConfirm: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = XColors.Surface,
        title = { Text("Set budget", color = XColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(XColors.SurfaceVariant).padding(14.dp)) {
                BasicTextField(
                    value = text, onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                    singleLine = true, textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = XColors.TextPrimary),
                    cursorBrush = SolidColor(XColors.Income), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner -> if (text.isEmpty()) Text("0", fontSize = 20.sp, color = XColors.TextSecondary); inner() }
                )
            }
        },
        confirmButton = { TextButton(onClick = { text.toDoubleOrNull()?.let(onConfirm) }) { Text("Save", color = XColors.Income, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = XColors.TextSecondary) } }
    )
}
