package com.example.s_journal.ui.canvas.engine

import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
import kotlin.math.hypot

class StrokeBuffer {

    private val _strokes = mutableListOf<Stroke>()
    val strokes: List<Stroke> get() = _strokes.toList()

    private val undoStack = mutableListOf<List<Stroke>>()
    private val redoStack = mutableListOf<List<Stroke>>()

    val canUndo: Boolean get() = undoStack.isNotEmpty() || _strokes.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun setStrokes(newStrokes: List<Stroke>) {
        _strokes.clear()
        _strokes.addAll(newStrokes)
        undoStack.clear()
        redoStack.clear()
    }

    fun addStroke(stroke: Stroke) {
        // Save snapshot to undo stack
        undoStack.add(_strokes.toList())
        redoStack.clear()
        _strokes.add(stroke)
    }

    fun undo(): Boolean {
        if (_strokes.isEmpty() && undoStack.isEmpty()) return false

        redoStack.add(_strokes.toList())
        if (undoStack.isNotEmpty()) {
            val previousState = undoStack.removeAt(undoStack.lastIndex)
            _strokes.clear()
            _strokes.addAll(previousState)
        } else {
            _strokes.clear()
        }
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false

        undoStack.add(_strokes.toList())
        val nextState = redoStack.removeAt(redoStack.lastIndex)
        _strokes.clear()
        _strokes.addAll(nextState)
        return true
    }

    fun clear() {
        if (_strokes.isNotEmpty()) {
            undoStack.add(_strokes.toList())
            redoStack.clear()
            _strokes.clear()
        }
    }

    /**
     * Erases any stroke that intersects with a circle around (x, y) with radius.
     * Returns true if any stroke was erased.
     */
    fun eraseStrokesIntersecting(x: Float, y: Float, radius: Float): Boolean {
        val strokesToRemove = mutableListOf<Stroke>()

        for (stroke in _strokes) {
            for (point in stroke.points) {
                val dist = hypot((point.x - x).toDouble(), (point.y - y).toDouble()).toFloat()
                if (dist <= (radius + stroke.baseWidth / 2f)) {
                    strokesToRemove.add(stroke)
                    break
                }
            }
        }

        if (strokesToRemove.isNotEmpty()) {
            undoStack.add(_strokes.toList())
            redoStack.clear()
            _strokes.removeAll(strokesToRemove)
            return true
        }

        return false
    }
}
