package com.example.s_journal.data.local.storage

import com.example.s_journal.ui.canvas.model.SpenTool
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class StrokeStorageImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storage: StrokeStorageImpl
    private val testDate: LocalDate = LocalDate.of(2026, 10, 16)

    @Before
    fun setUp() {
        storage = StrokeStorageImpl(tempFolder.root)
    }

    private fun createSampleStroke(id: String): Stroke {
        return Stroke(
            id = id,
            points = listOf(
                StrokePoint(x = 12.5f, y = 40.0f, pressure = 0.65f, timestamp = 100L),
                StrokePoint(x = 25.0f, y = 55.0f, pressure = 0.90f, timestamp = 116L)
            ),
            color = 0xFF2A85FF.toInt(),
            baseWidth = 6f,
            tool = SpenTool.PEN
        )
    }

    @Test
    fun saveStrokes_and_loadStrokes_persistsAndRestoresFromDisk() = runTest {
        val strokes = listOf(createSampleStroke("s1"), createSampleStroke("s2"))
        val relativePath = storage.saveStrokes(testDate, strokes)

        assertEquals("strokes/2026-10-16.sjink", relativePath)
        val expectedFile = File(tempFolder.root, "strokes/2026-10-16.sjink")
        assertTrue("Binary .sjink file must exist on disk", expectedFile.exists())

        val loaded = storage.loadStrokes(testDate)
        assertEquals(2, loaded.size)
        assertEquals("s1", loaded[0].id)
        assertEquals("s2", loaded[1].id)
        assertEquals(12.5f, loaded[0].points[0].x, 0.001f)
    }

    @Test
    fun saveStrokes_withEmptyList_deletesExistingFileAndReturnsEmptyString() = runTest {
        storage.saveStrokes(testDate, listOf(createSampleStroke("s1")))
        val file = File(tempFolder.root, "strokes/2026-10-16.sjink")
        assertTrue(file.exists())

        val pathAfterEmpty = storage.saveStrokes(testDate, emptyList())
        assertEquals("", pathAfterEmpty)
        assertFalse("File should be deleted when saving empty stroke list", file.exists())
    }

    @Test
    fun deleteStrokes_removesFileFromDisk() = runTest {
        storage.saveStrokes(testDate, listOf(createSampleStroke("s1")))
        val file = File(tempFolder.root, "strokes/2026-10-16.sjink")
        assertTrue(file.exists())

        storage.deleteStrokes(testDate)
        assertFalse(file.exists())
    }

    @Test
    fun loadStrokes_whenFileIsCorrupt_doesNotCrash_and_renamesToCorruptBackup() = runTest {
        val strokesDir = File(tempFolder.root, "strokes").apply { mkdirs() }
        val corruptFile = File(strokesDir, "$testDate.sjink")
        corruptFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04))

        val loaded = storage.loadStrokes(testDate)

        assertTrue("Corrupt file should gracefully return emptyList()", loaded.isEmpty())
        assertFalse("Original corrupt .sjink file should be moved aside", corruptFile.exists())

        val preservedCorrupt = File(strokesDir, "$testDate.sjink.corrupt")
        assertTrue("Corrupt file must be preserved as .corrupt for recovery", preservedCorrupt.exists())
    }
}
