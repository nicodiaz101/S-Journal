package com.example.s_journal.ui.canvas

import com.example.s_journal.ui.canvas.engine.StrokeSerializer
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
import com.example.s_journal.ui.components.SpenTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StrokeSerializerTest {

    @Test
    fun serializeAndDeserialize_emptyList_returnsEmptyList() {
        val bytes = StrokeSerializer.serialize(emptyList())
        val result = StrokeSerializer.deserialize(bytes)
        assertEquals(0, result.size)
    }

    @Test
    fun serializeAndDeserialize_multipleStrokes_restoresExactData() {
        val blueColor = 0xFF0000FF.toInt()
        val yellowColor = 0xFFFFFF00.toInt()

        val stroke1 = Stroke(
            id = "stroke-1",
            points = listOf(
                StrokePoint(x = 10f, y = 20f, pressure = 0.5f, timestamp = 1000L),
                StrokePoint(x = 15f, y = 25f, pressure = 0.8f, timestamp = 1010L)
            ),
            color = blueColor,
            baseWidth = 6.0f,
            tool = SpenTool.PEN
        )

        val stroke2 = Stroke(
            id = "stroke-2",
            points = listOf(
                StrokePoint(x = 100f, y = 200f, pressure = 1.0f, timestamp = 2000L)
            ),
            color = yellowColor,
            baseWidth = 20.0f,
            tool = SpenTool.HIGHLIGHTER
        )

        val originalStrokes = listOf(stroke1, stroke2)
        val bytes = StrokeSerializer.serialize(originalStrokes)
        assertNotNull(bytes)

        val deserializedStrokes = StrokeSerializer.deserialize(bytes)
        assertEquals(2, deserializedStrokes.size)

        // Validate stroke 1
        assertEquals("stroke-1", deserializedStrokes[0].id)
        assertEquals(SpenTool.PEN, deserializedStrokes[0].tool)
        assertEquals(blueColor, deserializedStrokes[0].color)
        assertEquals(6.0f, deserializedStrokes[0].baseWidth, 0.01f)
        assertEquals(2, deserializedStrokes[0].points.size)
        assertEquals(10f, deserializedStrokes[0].points[0].x, 0.01f)
        assertEquals(20f, deserializedStrokes[0].points[0].y, 0.01f)
        assertEquals(0.5f, deserializedStrokes[0].points[0].pressure, 0.01f)
        assertEquals(1000L, deserializedStrokes[0].points[0].timestamp)

        // Validate stroke 2
        assertEquals("stroke-2", deserializedStrokes[1].id)
        assertEquals(SpenTool.HIGHLIGHTER, deserializedStrokes[1].tool)
        assertEquals(yellowColor, deserializedStrokes[1].color)
        assertEquals(20.0f, deserializedStrokes[1].baseWidth, 0.01f)
        assertEquals(1, deserializedStrokes[1].points.size)
        assertEquals(100f, deserializedStrokes[1].points[0].x, 0.01f)
    }
}
