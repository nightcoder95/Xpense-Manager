package com.example.ui.feature.more

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.SectionHeader
import com.example.core.designsystem.XColors
import com.example.core.designsystem.XSurfaceCard
import com.example.ui.navigation.Routes

private data class GridAction(val label: String, val icon: ImageVector, val route: String?)

@Composable
fun MoreScreen(
    onNavigate: (String) -> Unit,
    onPlaceholder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val grid = listOf(
        GridAction("Transactions", Icons.AutoMirrored.Filled.ListAlt, Routes.CUSTOM),
        GridAction("Budgets", Icons.Filled.PieChart, Routes.BUDGETS),
        GridAction("Categories", Icons.Filled.Category, Routes.CATEGORIES),
        GridAction("Tags", Icons.Filled.Sell, Routes.TAGS)
    )
    val views = listOf(
        GridAction("Day", Icons.Filled.Today, Routes.DAY),
        GridAction("Calendar", Icons.Filled.CalendarMonth, Routes.CALENDAR),
        GridAction("Custom", Icons.Filled.Wallet, Routes.CUSTOM)
    )
    val options = listOf(
        "Settings" to Icons.Filled.Settings,
        "Invite friends" to Icons.Filled.Share,
        "Rate us" to Icons.Filled.Star,
        "Query / Support" to Icons.AutoMirrored.Filled.HelpOutline,
        "FAQs" to Icons.AutoMirrored.Filled.HelpOutline,
        "About" to Icons.Filled.Info
    )

    LazyColumn(
        modifier = modifier.fillMaxWidth().background(XColors.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("More", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = XColors.TextPrimary) }
        item { ProfileCard() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                grid.chunked(2).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { a ->
                            ActionTile(a, Modifier.weight(1f)) { a.route?.let(onNavigate) ?: onPlaceholder(a.label) }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item { SectionHeader("Views") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                views.forEach { a -> ActionTile(a, Modifier.weight(1f)) { a.route?.let(onNavigate) } }
            }
        }
        item {
            XSurfaceCard {
                options.forEachIndexed { i, (label, icon) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { if (label == "Settings") onNavigate(Routes.SETTINGS) else onPlaceholder(label) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = XColors.TextSecondary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(label, style = MaterialTheme.typography.titleMedium, color = XColors.TextPrimary)
                    }
                    if (i < options.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(XColors.Outline))
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ProfileCard() {
    XSurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(XColors.SurfaceVariant), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, null, tint = XColors.TextSecondary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Guest User", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
                Text("Sign in  •  Backup now", style = MaterialTheme.typography.bodyMedium, color = XColors.Indigo)
            }
        }
    }
}

@Composable
private fun ActionTile(a: GridAction, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(XColors.Surface).border(1.dp, XColors.Outline, RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(a.icon, null, tint = XColors.Indigo, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(8.dp))
        Text(a.label, style = MaterialTheme.typography.labelLarge, color = XColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}
