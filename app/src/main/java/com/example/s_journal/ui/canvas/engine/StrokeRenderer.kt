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
        renderPoints(
            canvas = canvas,
            points = stroke.points,
            color = stroke.color,
            baseWidth = stroke.baseWidth,
            tool = stroke.tool
        )
    }

    fun renderPoints(
        canvas: Canvas,
        points: List<StrokePoint>,
        color: Int,
        baseWidth: Float,
        tool: SpenTool
    ) {
        if (points.isEmpty()) return

        if (points.size == 1) {
            val point = points[0]
            val radius = calculateStrokeWidth(baseWidth, point.pressure, tool) / 2f
            dotPaint.color = color
            if (tool == SpenTool.HIGHLIGHTER) {
                dotPaint.alpha = 110 // Translucent highlighter
            } else {
                dotPaint.alpha = (color ushr 24)
            }
            canvas.drawCircle(point.x, point.y, radius, dotPaint)
            return
        }

        when (tool) {
            SpenTool.HIGHLIGHTER -> {
                renderHighlighterPoints(canvas, points, color, baseWidth)
            }
            SpenTool.PEN, SpenTool.PENCIL -> {
                renderPressureSensitivePoints(canvas, points, color, baseWidth, tool)
            }
            SpenTool.ERASER -> {
                // Eraser does not render directly; it erases strokes from the buffer
            }
        }
    }

    private fun renderHighlighterPoints(
        canvas: Canvas,
        points: List<StrokePoint>,
        color: Int,
        baseWidth: Float
    ) {
        highlighterPaint.color = color
        highlighterPaint.alpha = 100
        highlighterPaint.strokeWidth = baseWidth * 2.5f

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

    private fun renderPressureSensitivePoints(
        canvas: Canvas,
        points: List<StrokePoint>,
        color: Int,
        baseWidth: Float,
        tool: SpenTool
    ) {
        strokePaint.color = color

        val isPencil = tool == SpenTool.PENCIL
        if (isPencil) {
            strokePaint.alpha = 200 // Slightly textured/softer
        }

        // For smooth variable width, render segments connecting points
        for (i in 1 until points.size) {
            val p0 = points[i - 1]
            val p1 = points[i]

            val avgPressure = (p0.pressure + p1.pressure) / 2f
            val width = calculateStrokeWidth(baseWidth, avgPressure, tool)
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
