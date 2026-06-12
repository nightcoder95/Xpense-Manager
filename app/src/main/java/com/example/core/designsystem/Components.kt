package com.example.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.CurrencyFormatter

private val currency = CurrencyFormatter("₹")

/** Rounded surface container with a hairline outline — the standard card chrome. */
@Composable
fun XSurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(XColors.Surface)
            .border(1.dp, XColors.Outline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content
    )
}

/** Uppercase muted section label. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = XColors.TextSecondary,
        modifier = modifier
    )
}

/** Primary call-to-action button. */
@Composable
fun XPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = XColors.Income,
            contentColor = XColors.OnAccent,
            disabledContainerColor = XColors.Income.copy(alpha = 0.4f),
            disabledContentColor = XColors.OnAccent.copy(alpha = 0.6f)
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

/** Signed, color-coded monetary amount. `type` is one of EXPENSE/INCOME/TRANSFER. */
@Composable
fun AmountText(
    amount: Double,
    type: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    val prefix = when (type) {
        "EXPENSE" -> "-"
        "INCOME" -> "+"
        "TRANSFER" -> "⇄ "
        else -> ""
    }
    val color = when (type) {
        "EXPENSE" -> XColors.Spending
        "INCOME" -> XColors.Income
        "TRANSFER" -> XColors.Indigo
        else -> XColors.TextPrimary
    }
    Text(
        text = prefix + currency.format(amount),
        style = style,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        modifier = modifier
    )
}

/** Centered empty-state placeholder. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = XColors.TextSecondary.copy(alpha = 0.5f))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = XColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = XColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/** Rounded-capsule segmented toggle; the active option is highlighted. */
@Composable
fun XPillToggle(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(XColors.SurfaceVariant)
            .border(1.dp, XColors.Outline, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { opt ->
            val active = opt == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) XColors.Indigo.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(opt) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opt,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
                    color = if (active) XColors.TextPrimary else XColors.TextSecondary
                )
            }
        }
    }
}

/** Prev/next month stepper with a centered label. */
@Composable
fun MonthSelector(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            Icons.Filled.ChevronLeft, contentDescription = "Previous",
            tint = XColors.TextSecondary,
            modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onPrev() }
        )
        Text(label, style = MaterialTheme.typography.titleLarge, color = XColors.TextPrimary)
        Icon(
            Icons.Filled.ChevronRight, contentDescription = "Next",
            tint = XColors.TextSecondary,
            modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onNext() }
        )
    }
}

/** Title + uppercase subtitle header (e.g. "June 2026" / "12 TRANSACTIONS"). */
@Composable
fun DateRangeHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = XColors.TextPrimary)
        Spacer(Modifier.height(2.dp))
        SectionHeader(subtitle)
    }
}

/** Single transaction list row with a rounded-square category tile. */
@Composable
fun TransactionRow(
    category: String,
    account: String,
    note: String,
    amount: Double,
    type: String,
    time: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = category, tint = color, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                category, style = MaterialTheme.typography.titleMedium,
                color = XColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            val secondary = if (note.isNotBlank()) "$account  •  $note" else account
            Text(
                secondary, style = MaterialTheme.typography.bodyMedium,
                color = XColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            AmountText(amount = amount, type = type)
            Spacer(Modifier.height(2.dp))
            Text(time, style = MaterialTheme.typography.labelSmall, color = XColors.TextSecondary)
        }
    }
}

/**
 * Cash-flow summary card: SPENDING / INCOME headline amounts plus a Net Balance pill.
 * `trailing` renders an optional period control (e.g. "This Month ▾") in the header.
 */
@Composable
fun SummaryCard(
    spending: Double,
    income: Double,
    modifier: Modifier = Modifier,
    title: String = "CASH FLOW",
    trailing: (@Composable () -> Unit)? = null
) {
    val net = income - spending
    XSurfaceCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionHeader(title)
            if (trailing != null) trailing()
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("SPENDING", style = MaterialTheme.typography.labelSmall, color = XColors.Spending)
                Spacer(Modifier.height(4.dp))
                Text(
                    currency.format(spending),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = XColors.TextPrimary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("INCOME", style = MaterialTheme.typography.labelSmall, color = XColors.Income)
                Spacer(Modifier.height(4.dp))
                Text(
                    currency.format(income),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = XColors.TextPrimary
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(XColors.SurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Net Balance", style = MaterialTheme.typography.bodyMedium, color = XColors.TextSecondary)
            Text(
                currency.format(net),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (net >= 0) XColors.Income else XColors.Spending
            )
        }
    }
}

/** Selectable rounded-square category icon tile. */
@Composable
fun CategoryTile(
    name: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = name, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name, style = MaterialTheme.typography.labelSmall,
            color = if (selected) XColors.TextPrimary else XColors.TextSecondary,
            maxLines = 1
        )
    }
}
