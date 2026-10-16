package com.example.s_journal.ui.canvas.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
import com.example.s_journal.ui.canvas.model.SpenTool

class StrokeRenderer {

    private val basePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val path = Path()

    private val strokePaint = Paint(basePaint)
    private val dotPaint = Paint(basePaint).apply {
        style = Paint.Style.FILL
    }
    private val highlighterPaint = Paint(basePaint).apply {
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.BEVEL
    }

    fun renderStrokes(canvas: Canvas, strokes: List<Stroke>) {
        for (i in strokes.indices) {
            renderStroke(canvas, strokes[i])
        }
    }

    fun renderStroke(canvas: Canvas, stroke: Stroke) {
        val points = stroke.points
        if (points.isEmpty()) return

        if (points.size == 1) {
            val point = points[0]
            val radius = calculateStrokeWidth(stroke.baseWidth, point.pressure, stroke.tool) / 2f
            dotPaint.color = stroke.color
            if (stroke.tool == SpenTool.HIGHLIGHTER) {
                dotPaint.alpha = 110 // Translucent highlighter
            } else {
                dotPaint.alpha = (stroke.color ushr 24)
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
        highlighterPaint.color = stroke.color
        highlighterPaint.alpha = 100
        highlighterPaint.strokeWidth = stroke.baseWidth * 2.5f

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

        canvas.drawPath(path, highlighterPaint)
    }

    private fun renderPressureSensitiveStroke(canvas: Canvas, stroke: Stroke) {
        val points = stroke.points
        strokePaint.color = stroke.color

        val isPencil = stroke.tool == SpenTool.PENCIL
        if (isPencil) {
            strokePaint.alpha = 200 // Slightly textured/softer
        }

        // For smooth variable width, render segments connecting points
        for (i in 1 until points.size) {
            val p0 = points[i - 1]
            val p1 = points[i]

            val avgPressure = (p0.pressure + p1.pressure) / 2f
            val width = calculateStrokeWidth(stroke.baseWidth, avgPressure, stroke.tool)
            strokePaint.strokeWidth = width

            canvas.drawLine(p0.x, p0.y, p1.x, p1.y, strokePaint)
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
