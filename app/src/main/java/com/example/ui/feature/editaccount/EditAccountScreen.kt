package com.example.ui.feature.editaccount

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.designsystem.SectionHeader
import com.example.core.designsystem.XColors
import com.example.core.designsystem.XPillToggle
import com.example.core.designsystem.toComposeColor
import com.example.domain.model.AccountType

private val palette = listOf(
    "#43C59E", "#6C5DD3", "#F2706B", "#E3B341", "#5B8DEF",
    "#F08BBA", "#9B6DFF", "#3FBDBD", "#FF9F5A", "#7B8794"
)

private val typeLabels = mapOf(
    "Cash" to AccountType.CASH,
    "Bank" to AccountType.BANK,
    "Card" to AccountType.CREDIT_CARD,
    "Wallet" to AccountType.WALLET
)

@Composable
fun EditAccountScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accent = state.colorHex.toComposeColor()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    }

    Box(modifier.fillMaxSize().background(XColors.Background)) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(XColors.SurfaceVariant).clickable { onBack() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = XColors.TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(if (state.isEditing) "Edit account" else "New account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary, modifier = Modifier.weight(1f))
                    if (state.isEditing) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(XColors.Spending.copy(alpha = 0.15f)).clickable { viewModel.delete(onBack) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Delete, "Delete", tint = XColors.Spending, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            item {
                SectionHeader("Name")
                Spacer(Modifier.size(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(XColors.Surface).border(1.dp, XColors.Outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    BasicTextField(
                        value = state.name, onValueChange = viewModel::onName, singleLine = true,
                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = XColors.TextPrimary),
                        cursorBrush = SolidColor(accent), modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner -> if (state.name.isEmpty()) Text("Account name", fontSize = 18.sp, color = XColors.TextSecondary); inner() }
                    )
                }
            }
            item {
                SectionHeader("Type")
                Spacer(Modifier.size(8.dp))
                XPillToggle(
                    options = typeLabels.keys.toList(),
                    selected = typeLabels.entries.first { it.value == state.type }.key,
                    onSelect = { label -> typeLabels[label]?.let(viewModel::onType) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                SectionHeader("Opening balance")
                Spacer(Modifier.size(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(XColors.Surface).border(1.dp, XColors.Outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    BasicTextField(
                        value = state.openingBalanceText, onValueChange = viewModel::onBalance, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = XColors.TextPrimary),
                        cursorBrush = SolidColor(accent), modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner -> if (state.openingBalanceText.isEmpty()) Text("0", fontSize = 18.sp, color = XColors.TextSecondary); inner() }
                    )
                }
            }
            item {
                SectionHeader("Color")
                Spacer(Modifier.size(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    palette.take(5).forEach { ColorDot(it, state.colorHex == it) { viewModel.onColor(it) } }
                }
                Spacer(Modifier.size(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    palette.drop(5).forEach { ColorDot(it, state.colorHex == it) { viewModel.onColor(it) } }
                }
            }
            item { Spacer(Modifier.size(72.dp)) }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(24.dp).size(64.dp).clip(CircleShape)
                .background(if (state.name.isNotBlank()) XColors.Income else XColors.Income.copy(alpha = 0.4f))
                .clickable(enabled = state.name.isNotBlank()) { viewModel.save(onBack) },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Check, "Save", tint = XColors.OnAccent, modifier = Modifier.size(28.dp)) }
    }
}

@Composable
private fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    val c = hex.toComposeColor()
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(c)
            .border(if (selected) 3.dp else 0.dp, XColors.TextPrimary, CircleShape).clickable { onClick() }
    )
}
