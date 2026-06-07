package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatdrop.kt.ui.components.BackButton
import com.beatdrop.kt.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
)

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

object AppLog {
    private val _entries = mutableStateListOf<LogEntry>()
    val entries: List<LogEntry> get() = _entries.toList()

    fun v(tag: String, msg: String) = add(LogLevel.VERBOSE, tag, msg)
    fun d(tag: String, msg: String) = add(LogLevel.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = add(LogLevel.INFO, tag, msg)
    fun w(tag: String, msg: String, t: Throwable? = null) = add(LogLevel.WARN, tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) = add(LogLevel.ERROR, tag, msg, t)

    private fun add(level: LogLevel, tag: String, msg: String, t: Throwable? = null) {
        _entries.add(0, LogEntry(level = level, tag = tag, message = msg, throwable = t))
        if (_entries.size > 500) _entries.removeLast()
    }

    fun clear() { _entries.clear() }
}

@Composable
fun DebugLogScreen(
    onBack: () -> Unit = {},
) {
    val entries = AppLog.entries
    val errorCount = entries.count { it.level == LogLevel.ERROR }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(top = 90.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${entries.size} entries",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMedium),
                )
                if (errorCount > 0) {
                    Spacer(Modifier.width(12.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFF375F).copy(alpha = 0.15f)) {
                        Text(
                            "$errorCount errors",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { AppLog.clear() }) {
                    Icon(Icons.Filled.DeleteSweep, null, tint = TextLow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.bodySmall.copy(color = TextLow))
                }
            }

            // Log list
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Terminal, null, tint = TextHint, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No log entries", style = MaterialTheme.typography.bodyMedium.copy(color = TextLow))
                        Text("Errors and events will appear here", style = MaterialTheme.typography.bodySmall.copy(color = TextHint), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).padding(bottom = 40.dp)) {
                    items(entries, key = { it.timestamp }) { entry ->
                        val levelColor = when (entry.level) {
                            LogLevel.VERBOSE -> TextHint
                            LogLevel.DEBUG -> Color(0xFF64B5F6)
                            LogLevel.INFO -> Color(0xFF81C784)
                            LogLevel.WARN -> Color(0xFFFFB74D)
                            LogLevel.ERROR -> Color(0xFFFF6B6B)
                        }
                        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .then(
                                    if (entry.level == LogLevel.ERROR)
                                        Modifier.background(Color(0xFFFF375F).copy(alpha = 0.06f), RoundedCornerShape(6.dp)).padding(8.dp)
                                    else Modifier.padding(horizontal = 8.dp)
                                ),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    sdf.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = TextHint,
                                        fontSize = 10.sp,
                                    ),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    entry.level.name.take(1),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        color = levelColor,
                                        fontSize = 10.sp,
                                    ),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    entry.tag,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMedium,
                                        fontSize = 10.sp,
                                    ),
                                )
                            }
                            Text(
                                entry.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = levelColor,
                                    fontSize = 11.sp,
                                ),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            entry.throwable?.let { t ->
                                Text(
                                    t.stackTraceToString().take(200),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = TextLow,
                                        fontSize = 9.sp,
                                    ),
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text("Debug Log", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(Modifier.size(36.dp))
        }
    }
}
