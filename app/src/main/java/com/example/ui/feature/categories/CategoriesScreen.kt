package com.example.ui.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
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
import com.example.core.designsystem.CategoryTile
import com.example.core.designsystem.XColors
import com.example.core.designsystem.XSurfaceCard
import com.example.core.designsystem.toComposeColor
import com.example.domain.model.TxnType
import com.example.ui.util.CategoryIconHelper

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(modifier.fillMaxSize().background(XColors.Background)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(XColors.SurfaceVariant).clickable { onBack() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = XColors.TextPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
                    }
                    Spacer(Modifier.size(16.dp))
                    com.example.core.designsystem.XPillToggle(
                        options = listOf("Expense", "Income"),
                        selected = state.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        onSelect = { viewModel.setType(TxnType.valueOf(it.uppercase())) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(12.dp))
                    XSurfaceCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Default Category", style = MaterialTheme.typography.titleMedium, color = XColors.TextPrimary)
                                Text(state.defaultName ?: "None", style = MaterialTheme.typography.bodyMedium, color = XColors.TextSecondary)
                            }
                            Icon(Icons.Filled.ChevronRight, null, tint = XColors.TextSecondary)
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                }
            }
            items(state.categories, key = { it.name }) { c ->
                CategoryTile(
                    name = c.name,
                    icon = CategoryIconHelper.getCategoryIcon(c.iconName),
                    color = c.colorHex.toComposeColor(),
                    selected = c.isDefault,
                    onClick = { onEditCategory(c.name) }
                )
            }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(24.dp).size(56.dp).clip(CircleShape)
                .background(XColors.Income).clickable { onAddCategory() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Add, "Add category", tint = XColors.OnAccent) }
    }
}
