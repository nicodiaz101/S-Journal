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
import kotlinx.coroutines.launch
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
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
            ioDispatcher = testDispatcher
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
    fun initialState_hasDefaultToolAndEmptyStrokes() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SpenTool.PEN, state.currentTool)
        assertEquals(LocalDate.now(), state.selectedDate)
        assertEquals(0, state.strokes.size)
        assertFalse(state.canUndo)
    }

    @Test
    fun onToolSelected_updatesUiState() = runTest {
        viewModel.onToolSelected(SpenTool.HIGHLIGHTER)
        assertEquals(SpenTool.HIGHLIGHTER, viewModel.uiState.value.currentTool)

        viewModel.onToolSelected(SpenTool.PENCIL)
        assertEquals(SpenTool.PENCIL, viewModel.uiState.value.currentTool)
    }

    @Test
    fun onColorSelected_updatesUiState() = runTest {
        viewModel.onColorSelected(0xFF2A85FF.toInt())
        assertEquals(0xFF2A85FF.toInt(), viewModel.uiState.value.currentColor)
    }

    @Test
    fun onStrokesChanged_triggersDebounceSave() = runTest {
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

        val savedStrokes = fakeStorage.storage[LocalDate.now()]
        assertNotNull(savedStrokes)
        assertEquals(1, savedStrokes?.size)
        assertEquals("1", savedStrokes?.get(0)?.id)
        assertNotNull(fakeRepository.savedEntries[LocalDate.now().toString()])
    }

    @Test
    fun setDate_flushesPendingSaveAndLoadsNewDateStrokes() = runTest {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // Prepare yesterday with strokes in storage
        fakeStorage.storage[yesterday] = listOf(createDummyStroke("yesterday-stroke"))

        // Add a stroke to today without waiting for debounce
        viewModel.onStrokesChanged(listOf(createDummyStroke("today-stroke")))

        // Switch date to yesterday
        viewModel.setDate(yesterday)
        testDispatcher.scheduler.advanceUntilIdle()

        // Today's strokes should be flushed and saved
        assertEquals(1, fakeStorage.storage[today]?.size)
        assertEquals("today-stroke", fakeStorage.storage[today]?.get(0)?.id)

        // Yesterday's strokes should be loaded
        assertEquals(yesterday, viewModel.uiState.value.selectedDate)
        assertEquals(1, viewModel.uiState.value.strokes.size)
        assertEquals("yesterday-stroke", viewModel.uiState.value.strokes[0].id)
    }

    @Test
    fun clearCurrentCanvas_removesStrokesAndDeletesFromStorage() = runTest {
        val today = LocalDate.now()
        fakeStorage.storage[today] = listOf(createDummyStroke("stroke-to-clear"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearCurrentCanvas()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.strokes.size)
        assertFalse(viewModel.uiState.value.canUndo)
        assertEquals(null, fakeStorage.storage[today])
    }
}

class FakeStrokeStorage : StrokeStorage {
    val storage = mutableMapOf<LocalDate, List<Stroke>>()

    override suspend fun loadStrokes(date: LocalDate): List<Stroke> {
        return storage[date] ?: emptyList()
    }

    override suspend fun saveStrokes(date: LocalDate, strokes: List<Stroke>): String {
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
