package com.example.s_journal.ui.canvas

import com.example.s_journal.ui.canvas.engine.StrokeBuffer
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
import com.example.s_journal.ui.components.SpenTool
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
    fun clear_removesAllStrokes() {
        strokeBuffer.addStroke(createDummyStroke("1", 10f, 10f))
        strokeBuffer.addStroke(createDummyStroke("2", 20f, 20f))
        strokeBuffer.clear()
        assertEquals(0, strokeBuffer.strokes.size)
    }
}
