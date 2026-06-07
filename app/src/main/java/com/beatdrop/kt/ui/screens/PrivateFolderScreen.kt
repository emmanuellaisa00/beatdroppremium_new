package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.ui.components.Ic
import com.beatdrop.kt.data.DownloadHistory
import com.beatdrop.kt.ui.components.GlassHeader
import com.beatdrop.kt.ui.components.IconPuck
import com.beatdrop.kt.ui.components.ScreenScaffold
import com.beatdrop.kt.ui.components.glassCard
import com.beatdrop.kt.ui.components.glassRow
import com.beatdrop.kt.ui.theme.LocalAppColors
import com.beatdrop.kt.ui.theme.Radius
import com.beatdrop.kt.ui.theme.Spacing
import com.beatdrop.kt.ui.theme.Type
import com.beatdrop.kt.util.StorageHelper

@Composable
fun PrivateFolderScreen(
    savedPin: String?,
    onSetPin: (String) -> Unit,
    onBack: () -> Unit,
) {
    val C = LocalAppColors.current
    var enteredPin by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(savedPin == null) }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    if (savedPin == null || showSetPinDialog) {
        AlertDialog(
            onDismissRequest = { if (savedPin != null) showSetPinDialog = false },
            title = { Text(if (savedPin == null) "Set Private Folder PIN" else "Change PIN") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it; pinError = null },
                        label = { Text("New 4-digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it; pinError = null },
                        label = { Text("Confirm PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    pinError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = Type.caption)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
                        pinError = "PIN must be exactly 4 digits"
                    } else if (newPin != confirmPin) {
                        pinError = "PINs don't match"
                    } else {
                        onSetPin(newPin)
                        showSetPinDialog = false
                        isUnlocked = true
                    }
                }) { Text("Set PIN") }
            },
        )
    } else if (!isUnlocked) {
        // PIN entry screen
        ScreenScaffold(ambientColor = C.glassGlow, ambientIntensity = 0.16f) {
            Column(Modifier.fillMaxSize()) {
                GlassHeader(title = "Private Folder", onBack = onBack, leadingIcon = Ic.Lock)
                Column(
                    Modifier.fillMaxSize().padding(horizontal = Spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(48.dp))
                    IconPuck(icon = Ic.Lock, contentDescription = null, size = 96.dp, tint = C.accent)
                    Spacer(Modifier.height(20.dp))
                    Text("Private Folder", style = Type.title1, color = C.text)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enter your PIN to access private downloads",
                        style = Type.subhead, color = C.textSecondary,
                    )
                    Spacer(Modifier.height(24.dp))
                    Box(
                        Modifier.fillMaxWidth().glassCard(radius = Radius.lg).padding(16.dp),
                    ) {
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                enteredPin = it
                                if (it.length == 4 && it == savedPin) {
                                    isUnlocked = true
                                } else if (it.length == 4) {
                                    enteredPin = ""
                                }
                            },
                            label = { Text("4-digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radius.md),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row {
                        TextButton(onClick = onBack) { Text("Cancel") }
                        Spacer(Modifier.width(16.dp))
                        TextButton(onClick = { showSetPinDialog = true }) { Text("Change PIN") }
                    }
                }
            }
        }
    } else {
        // Private folder contents
        ScreenScaffold(ambientColor = C.glassAmbient) {
            Column(Modifier.fillMaxSize()) {
                GlassHeader(
                    title = "Private Folder",
                    onBack = { isUnlocked = false; onBack() },
                    leadingIcon = Ic.LockOpen,
                )

                val privateDownloads = DownloadHistory.getAll().filter { it.status == "completed" }
                if (privateDownloads.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconPuck(icon = Ic.Lock, contentDescription = null, size = 84.dp, tint = C.textTertiary)
                            Spacer(Modifier.height(16.dp))
                            Text("No private downloads yet", style = Type.headline, color = C.textSecondary)
                            Text(
                                "Mark downloads as private to hide them here",
                                style = Type.subhead, color = C.textTertiary,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = Spacing.lg),
                        contentPadding = PaddingValues(bottom = 190.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(privateDownloads, key = { it.videoId }) { record ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .glassRow()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Ic.MusicNote, null, tint = C.accent, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(record.title, style = Type.callout, color = C.text, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${StorageHelper.formatSize(record.fileSize)} · ${record.format}",
                                        style = Type.caption, color = C.textTertiary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
