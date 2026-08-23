package com.example.s_journal.ui.canvas.model

import com.example.s_journal.ui.components.SpenTool
import java.util.UUID

data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<StrokePoint>,
    val color: Int, // ARGB color integer
    val baseWidth: Float,
    val tool: SpenTool = SpenTool.PEN
)
