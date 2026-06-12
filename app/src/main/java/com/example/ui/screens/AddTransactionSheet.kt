package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.common.CurrencyFormatter
import com.example.core.designsystem.CategoryTile
import com.example.core.designsystem.XColors
import com.example.core.designsystem.XPillToggle
import com.example.core.designsystem.toComposeColor
import com.example.domain.model.TxnType
import com.example.domain.usecase.SaveTransactionUseCase
import com.example.domain.model.Category
import com.example.ui.FinanceViewModel
import com.example.ui.screens.addtransaction.AddTransactionViewModel
import com.example.ui.util.CategoryIconHelper
import com.example.ui.util.DateTimeUtils
import java.util.Calendar

private val currency = CurrencyFormatter("₹")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    categories: List<Category>,
    onClose: () -> Unit,
    onEditCategories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val editing by viewModel.editingTransaction.collectAsState()
    val addVm: AddTransactionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val type by addVm.type.collectAsState()
    val amountString by addVm.amount.collectAsState()
    val selectedCategory by addVm.category.collectAsState()
    val selectedPayment by addVm.paymentMode.collectAsState()
    val dateMillis by addVm.dateMillis.collectAsState()
    val note by addVm.note.collectAsState()
    val tag by addVm.tag.collectAsState()

    val accounts by viewModel.accounts.collectAsState()

    var categoryPickerOpen by remember { mutableStateOf(false) }
    var paymentPickerOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(editing) {
        if (editing != null) addVm.seedFrom(editing, viewModel.accountName(editing!!.accountId))
    }

    val typeLabels = listOf("Expense", "Income", "Transfer")
    val typeKey = type.lowercase().replaceFirstChar { it.uppercase() }

    val amt = amountString.toDoubleOrNull() ?: 0.0
    val txnType = runCatching { TxnType.valueOf(type) }.getOrDefault(TxnType.EXPENSE)
    val isValid = SaveTransactionUseCase.validate(amt, txnType, 1L, null).isValid
    val onSave: () -> Unit = {
        if (isValid) {
            viewModel.saveTransaction(type, amt, selectedCategory, selectedPayment, dateMillis, note, tag)
            addVm.reset()
        }
    }

    val typeAccent = when (type) {
        "INCOME" -> XColors.Income
        "TRANSFER" -> XColors.Indigo
        else -> XColors.Spending
    }

    Box(modifier.fillMaxSize().background(XColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(XColors.SurfaceVariant).clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = XColors.TextPrimary, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(16.dp))
                Text(
                    if (editing != null) "Edit transaction" else "Add transaction",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = XColors.TextPrimary, modifier = Modifier.weight(1f)
                )
                if (editing != null) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(XColors.Spending.copy(alpha = 0.15f))
                            .clickable { editing?.let { viewModel.deleteTransaction(it.id) }; onClose() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Delete, "Delete", tint = XColors.Spending, modifier = Modifier.size(20.dp)) }
                }
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                XPillToggle(typeLabels, typeKey, onSelect = { addVm.onTypeChange(it.uppercase()) }, modifier = Modifier.fillMaxWidth())

                // Date + time
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    PickerChip(Icons.Filled.DateRange, DateTimeUtils.formatDate(dateMillis)) {
                        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                        DatePickerDialog(context, { _, y, m, d ->
                            cal.set(y, m, d); addVm.onDateChange(cal.timeInMillis)
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }
                    PickerChip(Icons.Filled.Schedule, DateTimeUtils.formatTime(dateMillis)) {
                        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                        TimePickerDialog(context, { _, h, min ->
                            cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min); addVm.onDateChange(cal.timeInMillis)
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                    }
                }

                // Amount
                Column {
                    Text("Amount", style = MaterialTheme.typography.labelMedium, color = XColors.TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹", fontSize = 30.sp, fontWeight = FontWeight.Black, color = typeAccent)
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value = amountString,
                            onValueChange = { addVm.onAmountChange(it.filter { c -> c.isDigit() || c == '.' }) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, color = XColors.TextPrimary),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(typeAccent),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (amountString.isEmpty()) Text("0", fontSize = 34.sp, fontWeight = FontWeight.Black, color = XColors.TextSecondary)
                                inner()
                            }
                        )
                        Box(Modifier.size(40.dp).clip(CircleShape).background(XColors.SurfaceVariant), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Calculate, "Calculator", tint = XColors.TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Category row
                val cat = categories.firstOrNull { it.name == selectedCategory }
                val catColor = (cat?.colorHex ?: "#6C5DD3").toComposeColor()
                FieldRow(
                    label = "Category", value = selectedCategory,
                    icon = CategoryIconHelper.getCategoryIcon(cat?.iconName ?: "others"), iconColor = catColor,
                    onClick = { categoryPickerOpen = true }
                )

                // Payment row
                FieldRow(
                    label = "Payment mode", value = selectedPayment,
                    icon = paymentIcon(selectedPayment), iconColor = XColors.Income,
                    onClick = { paymentPickerOpen = true }
                )

                // Note
                Text("Other details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextSecondary)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notes, null, tint = XColors.TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    BasicTextField(
                        value = note, onValueChange = { addVm.onNoteChange(it) }, singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = XColors.TextPrimary),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(XColors.Indigo),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (note.isEmpty()) Text("Write a note", fontSize = 16.sp, color = XColors.TextSecondary)
                            inner()
                        }
                    )
                }

                // Tag chips
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("vacation", "amazon", "business").forEach { t ->
                        val sel = tag == t
                        Box(
                            Modifier.clip(RoundedCornerShape(16.dp))
                                .background(if (sel) XColors.Indigo else XColors.SurfaceVariant)
                                .clickable { addVm.onTagToggle(t) }.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("#$t", style = MaterialTheme.typography.labelMedium, color = if (sel) XColors.OnAccent else XColors.TextSecondary, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // White circular floppy Save FAB
        Box(
            Modifier.align(Alignment.BottomEnd).padding(24.dp).size(64.dp).clip(CircleShape)
                .background(if (isValid) Color.White else Color.White.copy(alpha = 0.4f))
                .clickable(enabled = isValid) { onSave() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Save, "Save", tint = XColors.Background, modifier = Modifier.size(28.dp)) }

        if (categoryPickerOpen) {
            CategoryPickerSheet(
                categories = categories.filter { it.type.name == type || type == "TRANSFER" },
                selected = selectedCategory,
                onPick = { addVm.onCategoryChange(it); categoryPickerOpen = false },
                onEdit = { categoryPickerOpen = false; onEditCategories() },
                onDismiss = { categoryPickerOpen = false }
            )
        }
        if (paymentPickerOpen) {
            AccountPickerSheet(
                modes = accounts.map { it.name }.ifEmpty { listOf("Cash") },
                selected = selectedPayment,
                onPick = { addVm.onPaymentChange(it); paymentPickerOpen = false },
                onDismiss = { paymentPickerOpen = false }
            )
        }
    }
}

@Composable
private fun PickerChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(XColors.SurfaceVariant).clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = XColors.TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
    }
}

@Composable
private fun FieldRow(
    label: String, value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.12f))
                .border(1.dp, iconColor.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, value, tint = iconColor, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = XColors.TextSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
        }
        Icon(Icons.Filled.ChevronRight, "Choose", tint = XColors.TextSecondary)
    }
}

private fun paymentIcon(mode: String) = when (mode) {
    "Bank Account" -> Icons.Filled.AccountBalance
    "Credit Card" -> Icons.Filled.CreditCard
    else -> Icons.Filled.Payments
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<Category>,
    selected: String,
    onPick: (String) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = XColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Select category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
                Icon(Icons.Filled.Edit, "Edit categories", tint = XColors.Indigo, modifier = Modifier.size(22.dp).clickable { onEdit() })
            }
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(categories, key = { it.name }) { c ->
                    CategoryTile(
                        name = c.name, icon = CategoryIconHelper.getCategoryIcon(c.iconName),
                        color = c.colorHex.toComposeColor(), selected = c.name == selected, onClick = { onPick(c.name) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    modes: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = XColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Payment mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            modes.forEach { mode ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (mode == selected) XColors.SurfaceVariant else Color.Transparent)
                        .clickable { onPick(mode) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(paymentIcon(mode), mode, tint = XColors.Income, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(mode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
