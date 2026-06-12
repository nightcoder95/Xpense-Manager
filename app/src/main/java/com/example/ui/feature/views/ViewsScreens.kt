package com.example.ui.feature.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.designsystem.DateRangeHeader
import com.example.core.designsystem.EmptyState
import com.example.core.designsystem.FilterSheet
import com.example.core.designsystem.MonthSelector
import com.example.core.designsystem.SummaryCard
import com.example.core.designsystem.TransactionRow
import com.example.core.designsystem.XColors
import com.example.core.designsystem.toComposeColor
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.ui.util.CategoryIconHelper
import com.example.ui.util.DateTimeUtils
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

@Composable
private fun txnRow(t: Transaction, cats: List<Category>, names: Map<Long, String>, onClick: (Long) -> Unit) {
    val cat = cats.firstOrNull { it.name == t.category }
    TransactionRow(
        category = t.category,
        account = names[t.accountId] ?: "",
        note = t.note,
        amount = t.amount,
        type = t.type.name,
        time = DateTimeUtils.formatTime(t.date),
        color = (cat?.colorHex ?: "#6C5DD3").toComposeColor(),
        icon = CategoryIconHelper.getCategoryIcon(cat?.iconName ?: "others"),
        onClick = { onClick(t.id) }
    )
}

@Composable
fun DayViewScreen(onTransactionClick: (Long) -> Unit, modifier: Modifier = Modifier, viewModel: ViewsViewModel = hiltViewModel()) {
    val (date, view) = viewModel.dayState.collectAsStateWithLifecycle().value
    val (cats, names) = viewModel.meta.collectAsStateWithLifecycle().value
    LazyColumn(modifier.fillMaxSize().background(XColors.Background), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(XColors.Surface).border(1.dp, XColors.Outline, RoundedCornerShape(20.dp)).padding(16.dp)) {
                MonthSelector(label = DateTimeUtils.formatDate(java.time.ZonedDateTime.of(date.atStartOfDay(), java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()), onPrev = viewModel::dayPrev, onNext = viewModel::dayNext)
            }
        }
        item { SummaryCard(spending = view.summary.spending, income = view.summary.income) }
        if (view.transactions.isEmpty()) {
            item { EmptyState(Icons.Filled.Search, "No transactions", "Nothing logged on this day.") }
        } else {
            items(view.transactions, key = { it.id }) { t -> txnRow(t, cats, names, onTransactionClick) }
        }
    }
}

@Composable
fun CalendarViewScreen(onDayClick: () -> Unit, modifier: Modifier = Modifier, viewModel: ViewsViewModel = hiltViewModel()) {
    val state by viewModel.calendarState.collectAsStateWithLifecycle()
    val first = state.anchor.withDayOfMonth(1)
    val firstWeekday = (first.dayOfWeek.value + 6) % 7 // Monday = 0
    val daysInMonth = state.anchor.lengthOfMonth()
    val cells = List(firstWeekday) { 0 } + (1..daysInMonth).toList()

    Column(modifier.fillMaxSize().background(XColors.Background).padding(16.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(XColors.Surface).border(1.dp, XColors.Outline, RoundedCornerShape(20.dp)).padding(16.dp)) {
            MonthSelector(
                label = "${state.anchor.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())} ${state.anchor.year}",
                onPrev = viewModel::monthPrev, onNext = viewModel::monthNext
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), color = XColors.TextSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(7), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            gridItems(cells) { day ->
                if (day == 0) Box(Modifier.aspectRatio(0.7f)) else {
                    val cd = state.days[day]
                    Column(
                        Modifier.aspectRatio(0.7f).clip(RoundedCornerShape(10.dp)).background(XColors.SurfaceVariant)
                            .clickable { viewModel.openDay(state.anchor.withDayOfMonth(day)); onDayClick() }.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("$day", style = MaterialTheme.typography.labelMedium, color = XColors.TextPrimary)
                        if (cd != null) {
                            if (cd.spending > 0) Text("-${cd.spending.toInt()}", fontSize = 8.sp, color = XColors.Spending, maxLines = 1)
                            if (cd.income > 0) Text("+${cd.income.toInt()}", fontSize = 8.sp, color = XColors.Income, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomViewScreen(onTransactionClick: (Long) -> Unit, modifier: Modifier = Modifier, viewModel: ViewsViewModel = hiltViewModel()) {
    val state by viewModel.customState.collectAsStateWithLifecycle()
    val (cats, names) = viewModel.meta.collectAsStateWithLifecycle().value
    var filterOpen by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(XColors.Background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(XColors.Surface).border(1.dp, XColors.Outline, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, tint = XColors.TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(12.dp))
                    BasicTextField(
                        value = state.filter.query, onValueChange = viewModel::setQuery, singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = XColors.TextPrimary), cursorBrush = SolidColor(XColors.Indigo),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner -> if (state.filter.query.isEmpty()) Text("Search transactions", color = XColors.TextSecondary); inner() }
                    )
                }
            }
            item { SummaryCard(spending = state.summary.spending, income = state.summary.income) }
            if (state.results.isEmpty()) {
                item { EmptyState(Icons.Filled.FilterList, "No matches", "No transactions match the current filters.") }
            } else {
                items(state.results, key = { it.id }) { t -> txnRow(t, cats, names, onTransactionClick) }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(24.dp).size(56.dp).clip(CircleShape).background(XColors.Indigo).clickable { filterOpen = true },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.FilterList, "Filter", tint = XColors.TextPrimary) }
    }

    if (filterOpen) {
        FilterSheet(
            categoryNames = state.allCategories.map { it.name },
            selectedCategories = state.filter.categories,
            accounts = state.allAccounts.map { it.id to it.name },
            selectedAccounts = state.filter.accountIds,
            onApply = { c, a -> viewModel.setFilter(state.filter.copy(categories = c, accountIds = a)) },
            onDismiss = { filterOpen = false }
        )
    }
}
