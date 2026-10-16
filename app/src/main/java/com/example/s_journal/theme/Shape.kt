package com.example.s_journal.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// One UI 9 Squircle / Superellipse Shape Tokens with continuous G2 curvature
val SJournalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = SquircleShape(12.dp),
    medium = SquircleShape(18.dp),
    large = SquircleShape(28.dp),
    extraLarge = SquircleShape(32.dp)
)

val PillShape = CircleShape
val CardSquircleShape = SquircleShape(28.dp)
val InnerCardShape = SquircleShape(20.dp)
val PanelSquircleShape = SquircleShape(topEnd = 24.dp, bottomEnd = 24.dp)
val ToolbarPillShape = RoundedCornerShape(50)
