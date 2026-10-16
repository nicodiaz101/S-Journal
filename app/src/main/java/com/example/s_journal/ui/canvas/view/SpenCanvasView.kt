package com.example.s_journal.ui.canvas.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.s_journal.ui.canvas.engine.StrokeBuffer
import com.example.s_journal.ui.canvas.engine.StrokeProcessor
import com.example.s_journal.ui.canvas.engine.StrokeRenderer
import com.example.s_journal.ui.canvas.model.SpenTool
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
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

    // Hardware backing bitmap: committed strokes are rendered once and drawn in O(1) time
    private var backingBitmap: Bitmap? = null
    private var backingCanvas: Canvas? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val oldBitmap = backingBitmap
            val newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val newCanvas = Canvas(newBitmap)
            backingBitmap = newBitmap
            backingCanvas = newCanvas
            oldBitmap?.recycle()
            rebuildBackingBitmap()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        backingBitmap?.recycle()
        backingBitmap = null
        backingCanvas = null
    }

    private fun rebuildBackingBitmap() {
        val bitmap = backingBitmap ?: return
        val canvas = backingCanvas ?: return
        bitmap.eraseColor(Color.TRANSPARENT)
        strokeRenderer.renderStrokes(canvas, strokeBuffer.strokes)
    }

    fun setStrokes(strokes: List<Stroke>) {
        strokeBuffer.setStrokes(strokes)
        rebuildBackingBitmap()
        invalidate()
    }

    fun undo() {
        if (strokeBuffer.undo()) {
            rebuildBackingBitmap()
            invalidate()
            onStrokesChangedListener?.invoke()
        }
    }

    fun redo() {
        if (strokeBuffer.redo()) {
            rebuildBackingBitmap()
            invalidate()
            onStrokesChangedListener?.invoke()
        }
    }

    fun clear() {
        strokeBuffer.clear()
        backingBitmap?.eraseColor(Color.TRANSPARENT)
        invalidate()
        onStrokesChangedListener?.invoke()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw all committed strokes from backing bitmap in O(1)
        val bitmap = backingBitmap
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        } else {
            strokeRenderer.renderStrokes(canvas, strokeBuffer.strokes)
        }

        // 2. Draw currently active stroke in progress
        if (activeStrokePoints.isNotEmpty() && !isErasing) {
            val activeStroke = Stroke(
                id = "active_stroke",
                points = activeStrokePoints,
                color = currentColor,
                baseWidth = currentStrokeWidth,
                tool = currentTool
            )
            strokeRenderer.renderStroke(canvas, activeStroke)
        }
    }

    private fun commitActiveStroke() {
        if (activeStrokePoints.isNotEmpty()) {
            val newStroke = Stroke(
                id = UUID.randomUUID().toString(),
                points = activeStrokePoints.toList(),
                color = currentColor,
                baseWidth = currentStrokeWidth,
                tool = currentTool
            )
            strokeBuffer.addStroke(newStroke)
            backingCanvas?.let { strokeRenderer.renderStroke(it, newStroke) }
            activeStrokePoints.clear()
            invalidate()
            onStrokesChangedListener?.invoke()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Track active stylus pointer by ID to avoid premature stroke termination if finger/palm touches
        var activeStylusIndex = -1
        for (i in 0 until event.pointerCount) {
            val toolType = event.getToolType(i)
            if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
                activeStylusIndex = i
                break
            }
        }

        // Strict Palm Rejection: If no S Pen stylus is present anywhere in this event, ignore
        if (strictPalmRejection && activeStylusIndex == -1) {
            return false
        }

        val actionIndex = event.actionIndex
        val actionMasked = event.actionMasked

        val pointerIndex = if (activeStylusIndex != -1) activeStylusIndex else 0
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

        when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Low latency: Request unbuffered dispatch to eliminate vsync batching delays
                requestUnbufferedDispatch(event)
                parent?.requestDisallowInterceptTouchEvent(true)

                if (effectiveTool == SpenTool.ERASER) {
                    isErasing = true
                    strokeBuffer.beginEraseGesture()
                    val erased = strokeBuffer.eraseStrokesIntersecting(x, y, currentStrokeWidth * 3f)
                    if (erased) {
                        rebuildBackingBitmap()
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
                        rebuildBackingBitmap()
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

            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (isErasing) {
                    strokeBuffer.endEraseGesture()
                    isErasing = false
                    onStrokesChangedListener?.invoke()
                } else if (effectiveTool != SpenTool.ERASER) {
                    commitActiveStroke()
                } else {
                    activeStrokePoints.clear()
                }
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Only commit stroke if the pointer that lifted was the stylus
                if (actionIndex == activeStylusIndex) {
                    if (isErasing) {
                        strokeBuffer.endEraseGesture()
                        isErasing = false
                        onStrokesChangedListener?.invoke()
                    } else if (effectiveTool != SpenTool.ERASER) {
                        commitActiveStroke()
                    } else {
                        activeStrokePoints.clear()
                    }
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (isErasing) {
                    strokeBuffer.cancelEraseGesture()
                    isErasing = false
                    rebuildBackingBitmap()
                }
                activeStrokePoints.clear()
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }
}
