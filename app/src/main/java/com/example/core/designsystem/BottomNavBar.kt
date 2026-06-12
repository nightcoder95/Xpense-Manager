package com.example.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Stateless 5-slot bottom bar: Home, Analysis, center [+] FAB, Accounts, More.
 * Tab keys are plain strings shared with the navigation graph.
 */
@Composable
fun BottomNavBar(
    current: String,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(XColors.Surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavSlot("home", "Home", Icons.Filled.Home, current, onSelect, Modifier.weight(1f))
        NavSlot("analysis", "Analysis", Icons.Filled.TrendingUp, current, onSelect, Modifier.weight(1f))

        // Center raised FAB.
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(XColors.TextPrimary)
                    .clickable { onAdd() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Add, contentDescription = "Add transaction",
                    tint = XColors.OnAccent, modifier = Modifier.size(28.dp)
                )
            }
        }

        NavSlot("accounts", "Accounts", Icons.Filled.AccountBalanceWallet, current, onSelect, Modifier.weight(1f))
        NavSlot("more", "More", Icons.Filled.MoreHoriz, current, onSelect, Modifier.weight(1f))
    }
}

@Composable
private fun NavSlot(
    route: String,
    label: String,
    icon: ImageVector,
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = current == route
    val tint = if (active) XColors.TextPrimary else XColors.TextSecondary
    Column(
        modifier = modifier.clip(CircleShape).clickable { onSelect(route) }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
