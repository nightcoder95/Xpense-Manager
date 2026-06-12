package com.example.ui.feature.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sell
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
import com.example.core.designsystem.EmptyState
import com.example.core.designsystem.XColors
import com.example.core.designsystem.XSurfaceCard

@Composable
fun TagsScreen(
    onBack: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagsViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    LazyColumn(modifier.fillMaxWidth().background(XColors.Background), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(XColors.SurfaceVariant).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = XColors.TextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text("Tags", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XColors.TextPrimary)
            }
        }
        if (tags.isEmpty()) {
            item { EmptyState(Icons.Filled.Sell, "No tags yet", "Tag transactions to group them here.") }
        } else {
            items(tags, key = { it.tag }) { tc ->
                XSurfaceCard(Modifier.clickable { onTagClick(tc.tag) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Sell, null, tint = XColors.Indigo, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Text("#${tc.tag}", style = MaterialTheme.typography.titleMedium, color = XColors.TextPrimary, modifier = Modifier.weight(1f))
                        Text("${tc.count}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XColors.TextSecondary)
                    }
                }
            }
        }
    }
}
