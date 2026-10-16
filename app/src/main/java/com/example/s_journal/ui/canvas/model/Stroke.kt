package com.example.s_journal.ui.canvas.model

import java.util.UUID

data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<StrokePoint>,
    val color: Int, // ARGB color integer
    val baseWidth: Float,
    val tool: SpenTool = SpenTool.PEN
) {
    val minX: Float
    val maxX: Float
    val minY: Float
    val maxY: Float

    init {
        var minXVal = Float.MAX_VALUE
        var maxXVal = -Float.MAX_VALUE
        var minYVal = Float.MAX_VALUE
        var maxYVal = -Float.MAX_VALUE
        for (pt in points) {
            if (pt.x < minXVal) minXVal = pt.x
            if (pt.x > maxXVal) maxXVal = pt.x
            if (pt.y < minYVal) minYVal = pt.y
            if (pt.y > maxYVal) maxYVal = pt.y
        }
        minX = if (points.isEmpty()) 0f else minXVal
        maxX = if (points.isEmpty()) 0f else maxXVal
        minY = if (points.isEmpty()) 0f else minYVal
        maxY = if (points.isEmpty()) 0f else maxYVal
    }
}
