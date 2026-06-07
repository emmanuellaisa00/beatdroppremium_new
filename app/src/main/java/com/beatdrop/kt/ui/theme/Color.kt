package com.beatdrop.kt.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ───── Core palette from premium HTML ─────
val Background = Color(0xFF050608)
val Surface = Color(0xFF0A0B0F)
val Accent = Color(0xFFFF375F)
val AccentDark = Color(0xFFB71F46)

// Glass system
val GlassBg = Color(0xB4121016)        // rgba(18,16,22,0.72)
val GlassBorder = Color(0x17FFFFFF)     // rgba(255,255,255,0.09)
val GlassHi = Color(0x1FFFFFFF)         // rgba(255,255,255,0.12)
val SurfaceTile = Color(0x12FFFFFF)     // rgba(255,255,255,0.07)
val SurfaceHover = Color(0x1CFFFFFF)    // rgba(255,255,255,0.11)
val DockBg = Color(0xCC0E0C12)         // rgba(14,12,18,0.80)
val MiniBg = Color(0xE0140C12)         // rgba(20,12,18,0.88)

// Text
val TextPrimary = Color.White
val TextHigh = Color(0xDDFFFFFF)       // 87%
val TextMedium = Color(0x94FFFFFF)     // 58%
val TextLow = Color(0x7AFFFFFF)        // 48%
val TextHint = Color(0x45FFFFFF)       // 27%

// Cover gradients (premium HTML c-1 through c-8)
object CoverGradients {
    val c1 = Brush.linearGradient(listOf(Color(0xFFff7788), Color(0xFFc73560), Color(0xFF4a1530)))
    val c2 = Brush.linearGradient(listOf(Color(0xFF2a80b0), Color(0xFF0152a0), Color(0xFF01253e)))
    val c3 = Brush.linearGradient(listOf(Color(0xFFffc55a), Color(0xFFd08520), Color(0xFF4a2c0a)))
    val c4 = Brush.linearGradient(listOf(Color(0xFFc09aff), Color(0xFF7550e2), Color(0xFF2c1a5e)))
    val c5 = Brush.linearGradient(listOf(Color(0xFF25d278), Color(0xFF0f9855), Color(0xFF064e2a)))
    val c6 = Brush.linearGradient(listOf(Color(0xFFff3d8e), Color(0xFF7a0fc4), Color(0xFF2c0458)))
    val c7 = Brush.linearGradient(listOf(Color(0xFF10dea8), Color(0xFF1598c0), Color(0xFF073e50)))
    val c8 = Brush.linearGradient(listOf(Color(0xFFf25a7a), Color(0xFFffcf70), Color(0xFFf88c6e)))
    fun get(i: Int) = when ((i % 8).let { if (it < 0) it + 8 else it }) { 1->c1;2->c2;3->c3;4->c4;5->c5;6->c6;7->c7;0->c8;else->c1 }
}

// Browse tile gradients
object TileGradients {
    val t1 = Brush.linearGradient(listOf(Color(0xFFff7a8a), Color(0xFFc73560)))
    val t2 = Brush.linearGradient(listOf(Color(0xFF2a80b0), Color(0xFF0152a0)))
    val t3 = Brush.linearGradient(listOf(Color(0xFF22d076), Color(0xFF0f9855)))
    val t4 = Brush.linearGradient(listOf(Color(0xFFc09aff), Color(0xFF7550e2)))
    val t5 = Brush.linearGradient(listOf(Color(0xFFffc55a), Color(0xFFd08520)))
    val t6 = Brush.linearGradient(listOf(Color(0xFFff3d8e), Color(0xFF7a0fc4)))
    val t7 = Brush.linearGradient(listOf(Color(0xFF10dea8), Color(0xFF1598c0)))
    val t8 = Brush.linearGradient(listOf(Color(0xFFf25a7a), Color(0xFFffcf70)))
    fun get(i: Int) = when ((i % 8).let { if (it < 0) it + 8 else it }) { 1->t1;2->t2;3->t3;4->t4;5->t5;6->t6;7->t7;0->t8;else->t1 }
}

// Screen background themes (breathing gradients)
object ScreenThemes {
    val home = Brush.verticalGradient(listOf(Color(0xFF0f1c23), Color(0xFF07090d), Color(0xFF000000)))
    val search = Brush.verticalGradient(listOf(Color(0xFF0f0e1e), Color(0xFF07070d), Color(0xFF000000)))
    val library = Brush.verticalGradient(listOf(Color(0xFF1e0f1a), Color(0xFF0b070e), Color(0xFF000000)))
    val nowPlaying = Brush.verticalGradient(listOf(Color(0xFF0f2640), Color(0xFF0a1828), Color(0xFF040a12)))
    val lyrics = Brush.verticalGradient(listOf(Color(0xFF0e2438), Color(0xFF091a2c), Color(0xFF040c18)))
}

// Accent gradient for progress bars
val AccentGradient = Brush.horizontalGradient(listOf(Accent, Color(0xFFff7a94)))
