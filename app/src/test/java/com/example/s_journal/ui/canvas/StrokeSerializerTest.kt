package com.example.s_journal.ui.canvas

import com.example.s_journal.ui.canvas.engine.StrokeSerializer
import com.example.s_journal.ui.canvas.model.SpenTool
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.EOFException

class StrokeSerializerTest {

    @Test
    fun serializeAndDeserialize_emptyList_returnsEmptyList() {
        val bytes = StrokeSerializer.serialize(emptyList())
        val result = StrokeSerializer.deserialize(bytes)
        assertEquals(0, result.size)
    }

    @Test
    fun deserialize_emptyByteArray_returnsEmptyList() {
        val result = StrokeSerializer.deserialize(ByteArray(0))
        assertTrue(result.isEmpty())
    }

    @Test
    fun serializeAndDeserialize_allTools_restoresExactData() {
        val strokes = SpenTool.entries.mapIndexed { index, tool ->
            Stroke(
                id = "stroke-$index",
                points = listOf(
                    StrokePoint(x = 10f * index, y = 20f * index, pressure = 0.5f, timestamp = 1000L + index),
                    StrokePoint(x = 15f * index, y = 25f * index, pressure = 0.8f, timestamp = 1010L + index)
                ),
                color = 0xFF0000FF.toInt() + index,
                baseWidth = 6.0f + index,
                tool = tool
            )
        }

        val bytes = StrokeSerializer.serialize(strokes)
        assertNotNull(bytes)

        val deserialized = StrokeSerializer.deserialize(bytes)
        assertEquals(strokes.size, deserialized.size)

        for (i in strokes.indices) {
            assertEquals(strokes[i].id, deserialized[i].id)
            assertEquals(strokes[i].tool, deserialized[i].tool)
            assertEquals(strokes[i].color, deserialized[i].color)
            assertEquals(strokes[i].baseWidth, deserialized[i].baseWidth, 0.001f)
            assertEquals(strokes[i].points.size, deserialized[i].points.size)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun deserialize_invalidMagicHeader_throwsIllegalArgumentException() {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeInt(0x12345678) // Wrong magic
            writeInt(1)
            writeInt(0)
            flush()
        }
        StrokeSerializer.deserialize(out.toByteArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun deserialize_unsupportedVersion_throwsIllegalArgumentException() {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeInt(0x534A494E) // "SJIN"
            writeInt(999)        // Unsupported version
            writeInt(0)
            flush()
        }
        StrokeSerializer.deserialize(out.toByteArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun deserialize_excessiveStrokeCount_throwsIllegalArgumentExceptionInsteadOfOOM() {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeInt(0x534A494E) // "SJIN"
            writeInt(1)
            writeInt(Int.MAX_VALUE) // Malicious/corrupted count
            flush()
        }
        StrokeSerializer.deserialize(out.toByteArray())
    }

    @Test(expected = EOFException::class)
    fun deserialize_truncatedPayload_throwsEOFException() {
        val validStroke = Stroke(
            id = "truncated",
            points = listOf(StrokePoint(1f, 2f, 0.5f, 100L)),
            color = 0,
            baseWidth = 4f,
            tool = SpenTool.PEN
        )
        val fullBytes = StrokeSerializer.serialize(listOf(validStroke))
        val truncatedBytes = fullBytes.copyOf(fullBytes.size - 6)
        StrokeSerializer.deserialize(truncatedBytes)
    }
}
