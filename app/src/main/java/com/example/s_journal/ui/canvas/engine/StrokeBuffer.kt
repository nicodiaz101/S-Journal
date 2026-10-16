package com.example.s_journal.ui.canvas.engine

import com.example.s_journal.ui.canvas.model.Stroke

class StrokeBuffer {

    companion object {
        private const val MAX_UNDO_STACK_SIZE = 50
    }

    private val _strokes = mutableListOf<Stroke>()
    val strokes: List<Stroke> get() = _strokes.toList()

    private val undoStack = mutableListOf<List<Stroke>>()
    private val redoStack = mutableListOf<List<Stroke>>()

    // Eraser gesture grouping
    private var erasingInitialState: List<Stroke>? = null
    private var hasErasedInCurrentGesture: Boolean = false

    val canUndo: Boolean get() = undoStack.isNotEmpty() || _strokes.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun setStrokes(newStrokes: List<Stroke>) {
        _strokes.clear()
        _strokes.addAll(newStrokes)
        undoStack.clear()
        redoStack.clear()
        erasingInitialState = null
        hasErasedInCurrentGesture = false
    }

    fun addStroke(stroke: Stroke) {
        pushUndoSnapshot(_strokes.toList())
        redoStack.clear()
        _strokes.add(stroke)
    }

    fun undo(): Boolean {
        if (_strokes.isEmpty() && undoStack.isEmpty()) return false

        pushRedoSnapshot(_strokes.toList())
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

        pushUndoSnapshot(_strokes.toList())
        val nextState = redoStack.removeAt(redoStack.lastIndex)
        _strokes.clear()
        _strokes.addAll(nextState)
        return true
    }

    fun clear() {
        if (_strokes.isNotEmpty()) {
            pushUndoSnapshot(_strokes.toList())
            redoStack.clear()
            _strokes.clear()
        }
    }

    /**
     * Call when an erase gesture begins (e.g. on ACTION_DOWN with eraser tool)
     * to group all strokes erased during this continuous gesture into a single undo step.
     */
    fun beginEraseGesture() {
        if (erasingInitialState == null) {
            erasingInitialState = _strokes.toList()
            hasErasedInCurrentGesture = false
        }
    }

    /**
     * Call when an erase gesture ends (e.g. on ACTION_UP) to commit the single undo step.
     */
    fun endEraseGesture() {
        val initialState = erasingInitialState
        if (initialState != null) {
            if (hasErasedInCurrentGesture) {
                pushUndoSnapshot(initialState)
                redoStack.clear()
            }
            erasingInitialState = null
            hasErasedInCurrentGesture = false
        }
    }

    /**
     * Call if an erase gesture was cancelled (e.g. on ACTION_CANCEL).
     */
    fun cancelEraseGesture() {
        val initialState = erasingInitialState
        if (initialState != null) {
            if (hasErasedInCurrentGesture) {
                _strokes.clear()
                _strokes.addAll(initialState)
            }
            erasingInitialState = null
            hasErasedInCurrentGesture = false
        }
    }

    /**
     * Erases any stroke that intersects with a circle around (x, y) with radius.
     * Uses bounding-box filtering and point-to-segment distance for robust, high-performance erasing.
     * Returns true if any stroke was erased.
     */
    fun eraseStrokesIntersecting(x: Float, y: Float, radius: Float): Boolean {
        val strokesToRemove = mutableListOf<Stroke>()

        for (stroke in _strokes) {
            val effectiveRadius = radius + stroke.baseWidth / 2f

            // 1. Fast bounding-box rejection
            if (x < stroke.minX - effectiveRadius || x > stroke.maxX + effectiveRadius ||
                y < stroke.minY - effectiveRadius || y > stroke.maxY + effectiveRadius
            ) {
                continue
            }

            // 2. Exact segment-level distance check
            val r2 = effectiveRadius * effectiveRadius
            var intersects = false
            val points = stroke.points

            if (points.size <= 1) {
                if (points.isNotEmpty()) {
                    val pt = points[0]
                    val dx = pt.x - x
                    val dy = pt.y - y
                    if (dx * dx + dy * dy <= r2) {
                        intersects = true
                    }
                }
            } else {
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    if (distanceSquaredToSegment(x, y, p1.x, p1.y, p2.x, p2.y) <= r2) {
                        intersects = true
                        break
                    }
                }
            }

            if (intersects) {
                strokesToRemove.add(stroke)
            }
        }

        if (strokesToRemove.isNotEmpty()) {
            if (erasingInitialState != null) {
                // Erase gesture active: record that erasure occurred, but defer undo snapshot until end of gesture
                hasErasedInCurrentGesture = true
            } else {
                // Standalone erase call: record undo snapshot immediately
                pushUndoSnapshot(_strokes.toList())
                redoStack.clear()
            }
            _strokes.removeAll(strokesToRemove)
            return true
        }

        return false
    }

    private fun pushUndoSnapshot(snapshot: List<Stroke>) {
        undoStack.add(snapshot)
        if (undoStack.size > MAX_UNDO_STACK_SIZE) {
            undoStack.removeAt(0)
        }
    }

    private fun pushRedoSnapshot(snapshot: List<Stroke>) {
        redoStack.add(snapshot)
        if (redoStack.size > MAX_UNDO_STACK_SIZE) {
            redoStack.removeAt(0)
        }
    }

    private fun distanceSquaredToSegment(
        px: Float, py: Float,
        ax: Float, ay: Float,
        bx: Float, by: Float
    ): Float {
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0f) {
            val dpx = px - ax
            val dpy = py - ay
            return dpx * dpx + dpy * dpy
        }
        val t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared
        val clampedT = t.coerceIn(0f, 1f)
        val projX = ax + clampedT * dx
        val projY = ay + clampedT * dy
        val diffX = px - projX
        val diffY = py - projY
        return diffX * diffX + diffY * diffY
    }
}
