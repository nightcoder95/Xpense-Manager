package com.example.ui.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.designsystem.AmountText
import com.example.core.designsystem.SummaryCard
import com.example.core.designsystem.XColors
import com.example.core.designsystem.toComposeColor
import com.example.domain.model.AccountBalance
import com.example.domain.model.AccountType

@Composable
fun AccountsScreen(
    onAddAccount: () -> Unit,
    onAccountClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AccountsContent(state, onAddAccount, onAccountClick, modifier)
}

@Composable
fun AccountsContent(
    state: AccountsUiState,
    onAddAccount: () -> Unit = {},
    onAccountClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().background(XColors.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Accounts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = XColors.TextPrimary) }
        item {
            // Net worth: positive balances as "income", negative as "spending" magnitude.
            val positive = state.balances.filter { it.balance >= 0 }.sumOf { it.balance }
            val negative = state.balances.filter { it.balance < 0 }.sumOf { -it.balance }
            SummaryCard(spending = negative, income = positive, title = "TOTAL NET WORTH")
        }
        items(state.balances, key = { it.account.id }) { ab -> AccountCard(ab) { onAccountClick(ab.account.id) } }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(XColors.SurfaceVariant)
                    .clickable { onAddAccount() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, null, tint = XColors.Income)
                Spacer(Modifier.width(12.dp))
                Text("Add account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AccountCard(ab: AccountBalance, onClick: () -> Unit) {
    val color = ab.account.colorHex.toComposeColor()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(XColors.Surface)
            .border(1.dp, XColors.Outline, RoundedCornerShape(20.dp)).clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(typeIcon(ab.account.type), ab.account.name, tint = color, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(ab.account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
            Text(ab.account.type.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = XColors.TextSecondary)
        }
        AmountText(amount = kotlin.math.abs(ab.balance), type = if (ab.balance >= 0) "INCOME" else "EXPENSE")
    }
}

private fun typeIcon(type: AccountType) = when (type) {
    AccountType.BANK -> Icons.Filled.AccountBalance
    AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
    AccountType.WALLET -> Icons.Filled.AccountBalanceWallet
    AccountType.CASH -> Icons.Filled.Payments
}
