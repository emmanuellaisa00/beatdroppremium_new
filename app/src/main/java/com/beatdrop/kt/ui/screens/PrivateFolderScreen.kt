package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.theme.*
import androidx.compose.ui.unit.sp

@Composable
fun PrivateFolderScreen(onBack: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var isSet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 96.dp).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isSet) "Enter PIN" else "Set PIN", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(if (isSet) "Unlock your private folder" else "Protect your private content with a PIN", color = TextMedium, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(32.dp))
            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { i ->
                    Box(modifier = Modifier.size(16.dp).background(if (i < pin.length) Accent else SurfaceTile, RoundedCornerShape(4.dp)))
                }
            }
            Spacer(Modifier.height(32.dp))
            // Number pad
            (0..2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    (1..3).forEach { col ->
                        val num = row * 3 + col
                        TextButton(onClick = { if (pin.length < 4) pin += num }, modifier = Modifier.size(64.dp)) {
                            Text("$num", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.size(64.dp))
                TextButton(onClick = { if (pin.length < 4) pin += "0" }, modifier = Modifier.size(64.dp)) { Text("0", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                TextButton(onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }, modifier = Modifier.size(64.dp)) { Text("⌫", color = TextMedium, fontSize = 24.sp) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Text("Private Folder", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
