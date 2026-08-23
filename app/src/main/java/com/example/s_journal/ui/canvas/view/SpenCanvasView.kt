package com.example.s_journal.ui.canvas.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.s_journal.ui.canvas.engine.StrokeBuffer
import com.example.s_journal.ui.canvas.engine.StrokeProcessor
import com.example.s_journal.ui.canvas.engine.StrokeRenderer
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
import com.example.s_journal.ui.components.SpenTool
import java.util.UUID

class SpenCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val strokeBuffer = StrokeBuffer()
    private val strokeProcessor = StrokeProcessor()
    private val strokeRenderer = StrokeRenderer()

    var currentTool: SpenTool = SpenTool.PEN
    var currentColor: Int = Color.BLACK
    var currentStrokeWidth: Float = 8f
    var onStrokesChangedListener: (() -> Unit)? = null

    // S Pen Exclusive Mode: ONLY accepts S Pen stylus events, completely rejects finger and palm touches
    var strictPalmRejection: Boolean = true

    private var activeStrokePoints = mutableListOf<StrokePoint>()
    private var isErasing = false

    init {
        // Hardware accelerated canvas
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setStrokes(strokes: List<Stroke>) {
        strokeBuffer.setStrokes(strokes)
        invalidate()
    }

    fun undo() {
        if (strokeBuffer.undo()) {
            invalidate()
            onStrokesChangedListener?.invoke()
        }
    }

    fun redo() {
        if (strokeBuffer.redo()) {
            invalidate()
            onStrokesChangedListener?.invoke()
        }
    }

    fun clear() {
        strokeBuffer.clear()
        invalidate()
        onStrokesChangedListener?.invoke()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw all committed strokes
        strokeRenderer.renderStrokes(canvas, strokeBuffer.strokes)

        // 2. Draw currently active stroke in progress
        if (activeStrokePoints.isNotEmpty() && !isErasing) {
            val activeStroke = Stroke(
                id = "active_stroke",
                points = activeStrokePoints.toList(),
                color = currentColor,
                baseWidth = currentStrokeWidth,
                tool = currentTool
            )
            strokeRenderer.renderStroke(canvas, activeStroke)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Find stylus pointer index
        var stylusPointerIndex = -1
        for (i in 0 until event.pointerCount) {
            val toolType = event.getToolType(i)
            if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
                stylusPointerIndex = i
                break
            }
        }

        // Strict Palm Rejection: If no S Pen stylus is detected in the touch event, reject immediately
        if (strictPalmRejection && stylusPointerIndex == -1) {
            return false
        }

        val pointerIndex = if (stylusPointerIndex != -1) stylusPointerIndex else 0
        val toolType = event.getToolType(pointerIndex)

        // S Pen primary side button held down switches to Eraser mode dynamically
        val isSpenButtonPressed = (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
        val effectiveTool = if (isSpenButtonPressed || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            SpenTool.ERASER
        } else {
            currentTool
        }

        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                if (effectiveTool == SpenTool.ERASER) {
                    isErasing = true
                    val erased = strokeBuffer.eraseStrokesIntersecting(x, y, currentStrokeWidth * 3f)
                    if (erased) {
                        invalidate()
                        onStrokesChangedListener?.invoke()
                    }
                } else {
                    isErasing = false
                    activeStrokePoints.clear()
                    val points = strokeProcessor.processHistoricalAndCurrentPoints(event, pointerIndex)
                    activeStrokePoints.addAll(points)
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isErasing || effectiveTool == SpenTool.ERASER) {
                    val erased = strokeBuffer.eraseStrokesIntersecting(x, y, currentStrokeWidth * 3f)
                    if (erased) {
                        invalidate()
                        onStrokesChangedListener?.invoke()
                    }
                } else {
                    val points = strokeProcessor.processHistoricalAndCurrentPoints(event, pointerIndex)
                    activeStrokePoints.addAll(points)
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (!isErasing && effectiveTool != SpenTool.ERASER && activeStrokePoints.isNotEmpty()) {
                    val newStroke = Stroke(
                        id = UUID.randomUUID().toString(),
                        points = activeStrokePoints.toList(),
                        color = currentColor,
                        baseWidth = currentStrokeWidth,
                        tool = currentTool
                    )
                    strokeBuffer.addStroke(newStroke)
                    activeStrokePoints.clear()
                    invalidate()
                    onStrokesChangedListener?.invoke()
                }
                isErasing = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                activeStrokePoints.clear()
                isErasing = false
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }
}
