package com.example.s_journal.ui.canvas

import com.example.s_journal.data.local.entity.JournalEntry
import com.example.s_journal.data.local.storage.StrokeStorage
import com.example.s_journal.domain.repository.JournalRepository
import com.example.s_journal.ui.canvas.model.SpenTool
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.canvas.model.StrokePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedInstant = Instant.parse("2026-10-16T12:00:00Z")
    private val testClock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
    private val expectedToday: LocalDate = LocalDate.of(2026, 10, 16)

    private lateinit var fakeStorage: FakeStrokeStorage
    private lateinit var fakeRepository: FakeJournalRepositoryForCanvas
    private lateinit var viewModel: CanvasViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeStorage = FakeStrokeStorage()
        fakeRepository = FakeJournalRepositoryForCanvas()
        viewModel = CanvasViewModel(
            journalRepository = fakeRepository,
            strokeStorage = fakeStorage,
            ioDispatcher = testDispatcher,
            clock = testClock
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createDummyStroke(id: String): Stroke {
        return Stroke(
            id = id,
            points = listOf(StrokePoint(10f, 10f, 1f)),
            color = 0xFF000000.toInt(),
            baseWidth = 6f,
            tool = SpenTool.PEN
        )
    }

    @Test
    fun initialState_hasDefaultToolAndDeterministicTodaySelected() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SpenTool.PEN, state.currentTool)
        assertEquals(expectedToday, state.selectedDate)
        assertEquals(0, state.strokes.size)
        assertFalse(state.canUndo)
        assertNull(state.errorMessage)
    }

    @Test
    fun onToolSelected_updatesUiState() = runTest {
        viewModel.onToolSelected(SpenTool.HIGHLIGHTER)
        assertEquals(SpenTool.HIGHLIGHTER, viewModel.uiState.value.currentTool)

        viewModel.onToolSelected(SpenTool.PENCIL)
        assertEquals(SpenTool.PENCIL, viewModel.uiState.value.currentTool)
    }

    @Test
    fun onColorAndWidthAndRuledLines_updatesUiState() = runTest {
        viewModel.onColorSelected(0xFF2A85FF.toInt())
        assertEquals(0xFF2A85FF.toInt(), viewModel.uiState.value.currentColor)

        viewModel.onStrokeWidthSelected(14f)
        assertEquals(14f, viewModel.uiState.value.currentStrokeWidth, 0.001f)

        val initialRuled = viewModel.uiState.value.showRuledLines
        viewModel.onToggleRuledLines()
        assertEquals(!initialRuled, viewModel.uiState.value.showRuledLines)
    }

    @Test
    fun onStrokesChanged_triggersDebounceSaveWithInjectedClockTimestamp() = runTest {
        val strokes = listOf(createDummyStroke("1"))
        viewModel.onStrokesChanged(strokes, canUndo = true, canRedo = false)

        assertTrue(viewModel.uiState.value.canUndo)
        assertEquals(1, viewModel.uiState.value.strokes.size)

        // Before debounce delay expires (500ms), nothing saved yet
        testDispatcher.scheduler.advanceTimeBy(300)
        assertEquals(0, fakeStorage.storage.size)

        // After debounce delay expires
        testDispatcher.scheduler.advanceTimeBy(250)
        testDispatcher.scheduler.advanceUntilIdle()

        val savedStrokes = fakeStorage.storage[expectedToday]
        assertNotNull(savedStrokes)
        assertEquals(1, savedStrokes?.size)
        assertEquals("1", savedStrokes?.get(0)?.id)

        val savedEntry = fakeRepository.savedEntries[expectedToday.toString()]
        assertNotNull(savedEntry)
        assertEquals(fixedInstant, savedEntry?.updatedAt)
    }

    @Test
    fun setDate_flushesPendingSaveAndLoadsNewDateStrokes() = runTest {
        val yesterday = expectedToday.minusDays(1)

        // Prepare yesterday with strokes in storage
        fakeStorage.storage[yesterday] = listOf(createDummyStroke("yesterday-stroke"))

        // Add a stroke to today without waiting for debounce
        viewModel.onStrokesChanged(listOf(createDummyStroke("today-stroke")))

        // Switch date to yesterday
        viewModel.setDate(yesterday)
        testDispatcher.scheduler.advanceUntilIdle()

        // Today's strokes should be flushed and saved
        assertEquals(1, fakeStorage.storage[expectedToday]?.size)
        assertEquals("today-stroke", fakeStorage.storage[expectedToday]?.get(0)?.id)

        // Yesterday's strokes should be loaded
        assertEquals(yesterday, viewModel.uiState.value.selectedDate)
        assertEquals(1, viewModel.uiState.value.strokes.size)
        assertEquals("yesterday-stroke", viewModel.uiState.value.strokes[0].id)
    }

    @Test
    fun rapidDateSwitching_loadsOnlyFinalSelectedDateStrokesWithoutRaceCondition() = runTest {
        val day1 = expectedToday.minusDays(1)
        val day2 = expectedToday.minusDays(2)
        val day3 = expectedToday.minusDays(3)

        fakeStorage.storage[day1] = listOf(createDummyStroke("d1-stroke"))
        fakeStorage.storage[day2] = listOf(createDummyStroke("d2-stroke"))
        fakeStorage.storage[day3] = listOf(createDummyStroke("d3-stroke"))

        viewModel.setDate(day1)
        viewModel.setDate(day2)
        viewModel.setDate(day3)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(day3, viewModel.uiState.value.selectedDate)
        assertEquals(1, viewModel.uiState.value.strokes.size)
        assertEquals("d3-stroke", viewModel.uiState.value.strokes[0].id)
    }

    @Test
    fun saveFailure_exposesErrorMessageAndResetsIsSavingWithoutCrashing() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        fakeStorage.shouldThrowOnSave = true

        viewModel.onStrokesChanged(listOf(createDummyStroke("fail-stroke")))
        testDispatcher.scheduler.advanceTimeBy(600)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isSaving must be reset to false after failure", state.isSaving)
        assertNotNull("errorMessage must be populated on I/O failure", state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Disk full"))
    }

    @Test
    fun loadFailure_exposesErrorMessageAndFallsBackToEmptyStrokes() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        fakeStorage.shouldThrowOnLoad = true

        val otherDay = expectedToday.plusDays(1)
        viewModel.setDate(otherDay)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.strokes.isEmpty())
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Read error"))
    }

    @Test
    fun clearCurrentCanvas_removesStrokesAndDeletesFromStorage() = runTest {
        fakeStorage.storage[expectedToday] = listOf(createDummyStroke("stroke-to-clear"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearCurrentCanvas()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.strokes.size)
        assertFalse(viewModel.uiState.value.canUndo)
        assertNull(fakeStorage.storage[expectedToday])
    }
}

class FakeStrokeStorage : StrokeStorage {
    val storage = mutableMapOf<LocalDate, List<Stroke>>()
    var shouldThrowOnSave = false
    var shouldThrowOnLoad = false

    override suspend fun loadStrokes(date: LocalDate): List<Stroke> {
        if (shouldThrowOnLoad) throw IOException("Read error")
        return storage[date] ?: emptyList()
    }

    override suspend fun saveStrokes(date: LocalDate, strokes: List<Stroke>): String {
        if (shouldThrowOnSave) throw IOException("Disk full")
        if (strokes.isEmpty()) {
            storage.remove(date)
            return ""
        }
        storage[date] = strokes
        return "strokes/$date.sjink"
    }

    override suspend fun deleteStrokes(date: LocalDate) {
        storage.remove(date)
    }
}

class FakeJournalRepositoryForCanvas : JournalRepository {
    val savedEntries = mutableMapOf<String, JournalEntry>()

    override fun getAllEntries(): Flow<List<JournalEntry>> = MutableStateFlow(savedEntries.values.toList())
    override fun getEntryById(id: String): Flow<JournalEntry?> = MutableStateFlow(savedEntries[id])
    override fun getEntryByDate(date: LocalDate): Flow<JournalEntry?> = MutableStateFlow(savedEntries.values.find { it.date == date })
    override suspend fun getEntryByDateSync(date: LocalDate): JournalEntry? = savedEntries.values.find { it.date == date }
    override fun getAllDatesWithEntries(): Flow<List<LocalDate>> = MutableStateFlow(savedEntries.values.map { it.date })
    override fun getEntriesInDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<JournalEntry>> =
        MutableStateFlow(savedEntries.values.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) })

    override suspend fun saveEntry(entry: JournalEntry) {
        savedEntries[entry.id] = entry
    }

    override suspend fun updateEntry(entry: JournalEntry) {
        savedEntries[entry.id] = entry
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        savedEntries.remove(entry.id)
    }

    override suspend fun deleteEntryById(id: String) {
        savedEntries.remove(id)
    }
}
