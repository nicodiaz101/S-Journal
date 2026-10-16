package com.example.s_journal.ui.canvas

import com.example.s_journal.ui.canvas.engine.StrokeBuffer
import com.example.s_journal.ui.canvas.model.SpenTool
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StrokeBufferTest {

    private lateinit var strokeBuffer: StrokeBuffer

    @Before
    fun setUp() {
        strokeBuffer = StrokeBuffer()
    }

    private fun createDummyStroke(id: String, x: Float, y: Float): Stroke {
        return Stroke(
            id = id,
            points = listOf(StrokePoint(x = x, y = y, pressure = 1.0f)),
            color = 0xFF000000.toInt(),
            baseWidth = 8.0f,
            tool = SpenTool.PEN
        )
    }

    @Test
    fun addStroke_increasesCountAndEnablesUndo() {
        assertFalse(strokeBuffer.canUndo)
        strokeBuffer.addStroke(createDummyStroke("1", 10f, 10f))
        assertEquals(1, strokeBuffer.strokes.size)
        assertTrue(strokeBuffer.canUndo)
    }

    @Test
    fun undoAndRedo_cyclesStateCorrectly() {
        strokeBuffer.addStroke(createDummyStroke("1", 10f, 10f))
        strokeBuffer.addStroke(createDummyStroke("2", 20f, 20f))
        assertEquals(2, strokeBuffer.strokes.size)

        // Undo stroke 2
        val undo1 = strokeBuffer.undo()
        assertTrue(undo1)
        assertEquals(1, strokeBuffer.strokes.size)
        assertEquals("1", strokeBuffer.strokes[0].id)
        assertTrue(strokeBuffer.canRedo)

        // Redo stroke 2
        val redo1 = strokeBuffer.redo()
        assertTrue(redo1)
        assertEquals(2, strokeBuffer.strokes.size)
        assertEquals("2", strokeBuffer.strokes[1].id)
    }

    @Test
    fun eraseStrokesIntersecting_removesTargetStroke() {
        val s1 = createDummyStroke("1", 100f, 100f)
        val s2 = createDummyStroke("2", 300f, 300f)
        strokeBuffer.addStroke(s1)
        strokeBuffer.addStroke(s2)

        // Erase around (100, 100)
        val erased = strokeBuffer.eraseStrokesIntersecting(102f, 98f, radius = 20f)
        assertTrue(erased)
        assertEquals(1, strokeBuffer.strokes.size)
        assertEquals("2", strokeBuffer.strokes[0].id)
    }

    @Test
    fun eraseGesture_groupsMultipleErasuresIntoSingleUndoStep() {
        val s1 = createDummyStroke("1", 100f, 100f)
        val s2 = createDummyStroke("2", 150f, 100f)
        val s3 = createDummyStroke("3", 300f, 300f)
        strokeBuffer.addStroke(s1)
        strokeBuffer.addStroke(s2)
        strokeBuffer.addStroke(s3)
        assertEquals(3, strokeBuffer.strokes.size)

        // Begin a single erase sweep
        strokeBuffer.beginEraseGesture()
        strokeBuffer.eraseStrokesIntersecting(100f, 100f, radius = 10f)
        strokeBuffer.eraseStrokesIntersecting(150f, 100f, radius = 10f)
        strokeBuffer.endEraseGesture()

        assertEquals(1, strokeBuffer.strokes.size)
        assertEquals("3", strokeBuffer.strokes[0].id)

        // Undoing should restore BOTH erased strokes in a single step
        val undone = strokeBuffer.undo()
        assertTrue(undone)
        assertEquals(3, strokeBuffer.strokes.size)
    }

    @Test
    fun eraseStrokesIntersecting_detectsIntersectionAlongSegment() {
        // Line between (0, 0) and (100, 0) with no intermediate points
        val longSegmentStroke = Stroke(
            id = "segment",
            points = listOf(
                StrokePoint(x = 0f, y = 0f, pressure = 1.0f),
                StrokePoint(x = 100f, y = 0f, pressure = 1.0f)
            ),
            color = 0xFF000000.toInt(),
            baseWidth = 4.0f,
            tool = SpenTool.PEN
        )
        strokeBuffer.addStroke(longSegmentStroke)

        // Eraser passes through midpoint (50, 0)
        val erased = strokeBuffer.eraseStrokesIntersecting(50f, 0f, radius = 5f)
        assertTrue("Eraser should intersect midpoint of segment", erased)
        assertEquals(0, strokeBuffer.strokes.size)
    }

    @Test
    fun clear_removesAllStrokes() {
        strokeBuffer.addStroke(createDummyStroke("1", 10f, 10f))
        strokeBuffer.addStroke(createDummyStroke("2", 20f, 20f))
        strokeBuffer.clear()
        assertEquals(0, strokeBuffer.strokes.size)
    }
}
