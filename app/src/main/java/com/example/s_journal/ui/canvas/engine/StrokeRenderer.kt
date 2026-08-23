package com.example.s_journal.ui.canvas.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
import com.example.s_journal.ui.components.SpenTool

class StrokeRenderer {

    private val basePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val path = Path()

    fun renderStrokes(canvas: Canvas, strokes: List<Stroke>) {
        for (stroke in strokes) {
            renderStroke(canvas, stroke)
        }
    }

    fun renderStroke(canvas: Canvas, stroke: Stroke) {
        val points = stroke.points
        if (points.isEmpty()) return

        if (points.size == 1) {
            val point = points[0]
            val radius = calculateStrokeWidth(stroke.baseWidth, point.pressure, stroke.tool) / 2f
            val dotPaint = Paint(basePaint).apply {
                color = stroke.color
                style = Paint.Style.FILL
                if (stroke.tool == SpenTool.HIGHLIGHTER) {
                    alpha = 110 // Translucent highlighter
                }
            }
            canvas.drawCircle(point.x, point.y, radius, dotPaint)
            return
        }

        when (stroke.tool) {
            SpenTool.HIGHLIGHTER -> {
                renderHighlighterStroke(canvas, stroke)
            }
            SpenTool.PEN, SpenTool.PENCIL -> {
                renderPressureSensitiveStroke(canvas, stroke)
            }
            SpenTool.ERASER -> {
                // Eraser does not render directly; it erases strokes from the buffer
            }
        }
    }

    private fun renderHighlighterStroke(canvas: Canvas, stroke: Stroke) {
        val points = stroke.points
        val paint = Paint(basePaint).apply {
            color = stroke.color
            alpha = 100
            strokeWidth = stroke.baseWidth * 2.5f
            strokeCap = Paint.Cap.SQUARE
            strokeJoin = Paint.Join.BEVEL
        }

        path.reset()
        path.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size) {
            val p0 = points[i - 1]
            val p1 = points[i]
            val midX = (p0.x + p1.x) / 2f
            val midY = (p0.y + p1.y) / 2f
            path.quadTo(p0.x, p0.y, midX, midY)
        }
        val lastPoint = points.last()
        path.lineTo(lastPoint.x, lastPoint.y)

        canvas.drawPath(path, paint)
    }

    private fun renderPressureSensitiveStroke(canvas: Canvas, stroke: Stroke) {
        val points = stroke.points

        // For smooth variable width, render segments connecting points
        for (i in 1 until points.size) {
            val p0 = points[i - 1]
            val p1 = points[i]

            val avgPressure = (p0.pressure + p1.pressure) / 2f
            val width = calculateStrokeWidth(stroke.baseWidth, avgPressure, stroke.tool)

            val segmentPaint = Paint(basePaint).apply {
                color = stroke.color
                strokeWidth = width
                if (stroke.tool == SpenTool.PENCIL) {
                    alpha = 200 // Slightly textured/softer
                }
            }

            canvas.drawLine(p0.x, p0.y, p1.x, p1.y, segmentPaint)
        }
    }

    private fun calculateStrokeWidth(baseWidth: Float, pressure: Float, tool: SpenTool): Float {
        return when (tool) {
            SpenTool.PEN -> {
                // Modulate thickness: 40% at min pressure to 160% at max pressure
                baseWidth * (0.4f + (1.2f * pressure.coerceIn(0.1f, 1.0f)))
            }
            SpenTool.PENCIL -> {
                baseWidth * (0.6f + (0.8f * pressure.coerceIn(0.1f, 1.0f)))
            }
            SpenTool.HIGHLIGHTER -> {
                baseWidth * 2.5f
            }
            SpenTool.ERASER -> {
                baseWidth * 3f
            }
        }
    }
}
