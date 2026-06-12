package com.example.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Reusable transaction filter sheet — replaces the hardcoded chips of A2#12.
 * Operates on primitive display data so it stays free of any ui-layer dependency.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    categoryNames: List<String>,
    selectedCategories: Set<String>,
    accounts: List<Pair<Long, String>>,
    selectedAccounts: Set<Long>,
    onApply: (Set<String>, Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var cats by remember { mutableStateOf(selectedCategories) }
    var accs by remember { mutableStateOf(selectedAccounts) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = XColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionHeader("Categories")
                Text(
                    if (cats.isEmpty()) "Select all" else "Clear",
                    style = MaterialTheme.typography.labelMedium, color = XColors.Indigo,
                    modifier = Modifier.clickable { cats = if (cats.isEmpty()) categoryNames.toSet() else emptySet() }
                )
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryNames.forEach { name ->
                    Chip(name, name in cats) { cats = if (name in cats) cats - name else cats + name }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("Payment modes")
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { (id, name) ->
                    Chip(name, id in accs) { accs = if (id in accs) accs - id else accs + id }
                }
            }

            Spacer(Modifier.height(24.dp))
            XPrimaryButton("Apply Filters", onClick = { onApply(cats, accs); onDismiss() })
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(16.dp))
            .background(if (selected) XColors.Indigo.copy(alpha = 0.2f) else XColors.SurfaceVariant)
            .border(1.dp, if (selected) XColors.Indigo else XColors.Outline, RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) XColors.TextPrimary else XColors.TextSecondary, fontWeight = FontWeight.Bold)
    }
}
