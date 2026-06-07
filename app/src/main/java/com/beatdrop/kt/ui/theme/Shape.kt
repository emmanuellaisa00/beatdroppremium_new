package com.beatdrop.kt.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val BeatDropShapes = Shapes(
    small = RoundedCornerShape(4.dp),     // track art
    medium = RoundedCornerShape(8.dp),     // track rows, pills
    large = RoundedCornerShape(14.dp),     // cards, now-playing cover
    extraLarge = RoundedCornerShape(32.dp) // dock, search bar, mini player
)
