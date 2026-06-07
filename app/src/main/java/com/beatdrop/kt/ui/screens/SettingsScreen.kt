package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenEq: () -> Unit = {},
    onOpenStorage: () -> Unit = {},
    onOpenPrivateFolder: () -> Unit = {},
    onOpenDebugLog: () -> Unit = {},
) {
    val items = listOf(
        Triple(Icons.Filled.Equalizer, "Equalizer", "Audio settings & presets") to onOpenEq,
        Triple(Icons.Filled.Storage, "Storage", "Manage downloads & cache") to onOpenStorage,
        Triple(Icons.Filled.Lock, "Private Folder", "PIN-protected content") to onOpenPrivateFolder,
        Triple(Icons.Filled.Info, "About", "Version, licenses") to {},
        Triple(Icons.Filled.BugReport, "Debug Log", "View app logs") to onOpenDebugLog,
    )

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 96.dp, bottom = 40.dp)) {
            items(items.size) { i ->
                val (data, action) = items[i]
                val (icon, title, subtitle) = data
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = action).padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Box(modifier = Modifier.size(40.dp).background(SurfaceTile, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = TextHigh, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(subtitle, fontWeight = FontWeight.Medium, color = TextLow, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    }
                    Text("›", color = Color.White.copy(alpha = 0.35f), fontWeight = FontWeight.Light, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
