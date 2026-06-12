package com.example.ui.feature.settings

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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.designsystem.SectionHeader
import com.example.core.designsystem.XColors
import com.example.core.designsystem.XSurfaceCard

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> confirmRestoreUri = uri }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }

    LazyColumn(modifier.fillMaxWidth().background(XColors.Background), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(XColors.SurfaceVariant).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = XColors.TextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
            }
        }
        item {
            SettingsGroup("Appearance") {
                SwitchRow("Dark theme", prefs.darkTheme, viewModel::setDarkTheme)
                SwitchRow("Show decimals", prefs.showDecimals, viewModel::setShowDecimals)
                SwitchRow("Haptic feedback", prefs.haptics, viewModel::setHaptics)
            }
        }
        item {
            SettingsGroup("Preferences") {
                InfoRow("Currency & Format", prefs.currencySymbol)
                InfoRow("Default Payment", prefs.defaultPayment)
            }
        }
        item {
            SettingsGroup("Backup & Restore") {
                ActionRow("Export backup (JSON)") {
                    exportLauncher.launch("xpense-backup-${System.currentTimeMillis()}.json")
                }
                ActionRow("Restore from backup") {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            }
        }
        item {
            SettingsGroup("Danger Zone") {
                Text(
                    "Delete all transaction data",
                    style = MaterialTheme.typography.titleMedium, color = XColors.Spending, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().clickable { confirmDelete = true }.padding(vertical = 12.dp)
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    confirmRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestoreUri = null },
            containerColor = XColors.Surface,
            title = { Text("Restore backup?", color = XColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This wipes ALL current data (transactions, accounts, categories, budgets) and replaces it with the backup contents. This cannot be undone.", color = XColors.TextSecondary) },
            confirmButton = { TextButton(onClick = { viewModel.importFrom(uri); confirmRestoreUri = null }) { Text("Restore", color = XColors.Spending, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmRestoreUri = null }) { Text("Cancel", color = XColors.TextSecondary) } }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = XColors.Surface,
            title = { Text("Delete all data?", color = XColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This permanently removes every transaction. This cannot be undone.", color = XColors.TextSecondary) },
            confirmButton = { TextButton(onClick = { viewModel.deleteAllData(); confirmDelete = false }) { Text("Delete", color = XColors.Spending, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = XColors.TextSecondary) } }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        SectionHeader(title)
        Spacer(Modifier.height(8.dp))
        XSurfaceCard { content() }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = XColors.TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = XColors.Income, checkedThumbColor = XColors.OnAccent)
        )
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium, color = XColors.TextPrimary, fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = XColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = XColors.TextSecondary)
    }
}
