package com.example.s_journal.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// One UI 9 Squircle / Superellipse Shape Tokens
val SJournalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val PillShape = CircleShape
val CardSquircleShape = RoundedCornerShape(28.dp)
val InnerCardShape = RoundedCornerShape(20.dp)
val ToolbarPillShape = RoundedCornerShape(50)
