package com.example.s_journal.ui.calendar

import com.example.s_journal.data.local.entity.JournalEntry
import com.example.s_journal.data.model.EntryType
import com.example.s_journal.domain.repository.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeJournalRepository
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeJournalRepository()
        viewModel = CalendarViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasCurrentMonthAndTodaySelected() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocalDate.now(), state.selectedDate)
        assertEquals(YearMonth.now(), state.currentMonth)
    }

    @Test
    fun onSelectDate_updatesSelectedDateAndMonth() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val targetDate = LocalDate.of(2026, 12, 25)
        viewModel.onSelectDate(targetDate)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(targetDate, state.selectedDate)
        assertEquals(YearMonth.of(2026, 12), state.currentMonth)
    }

    @Test
    fun onPreviousMonth_and_onNextMonth_navigateCorrectly() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val initialMonth = viewModel.uiState.value.currentMonth

        viewModel.onPreviousMonth()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(initialMonth.minusMonths(1), viewModel.uiState.value.currentMonth)

        viewModel.onNextMonth()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(initialMonth, viewModel.uiState.value.currentMonth)
    }

    @Test
    fun onGoToToday_resetsToCurrentDateAndMonth() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSelectDate(LocalDate.of(2024, 1, 1))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onGoToToday()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
        assertEquals(YearMonth.now(), viewModel.uiState.value.currentMonth)
    }

    @Test
    fun datesWithEntries_areReflectedInUiState() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val testDate = LocalDate.of(2026, 8, 23)
        fakeRepository.setDatesWithEntries(listOf(testDate))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.datesWithEntries.contains(testDate))
    }

    @Test
    fun selectedDateEntry_isLoadedWhenPresent() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val testDate = LocalDate.of(2026, 8, 23)
        val testEntry = JournalEntry(
            id = "entry-1",
            date = testDate,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            type = EntryType.TEXT,
            title = "Test Title",
            textContent = "Test Content"
        )
        fakeRepository.setEntryForDate(testDate, testEntry)
        viewModel.onSelectDate(testDate)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.selectedDateEntry)
        assertEquals("entry-1", state.selectedDateEntry?.id)
        assertEquals("Test Title", state.selectedDateEntry?.title)
    }
}

private class FakeJournalRepository : JournalRepository {
    private val entriesFlow = MutableStateFlow<Map<LocalDate, JournalEntry>>(emptyMap())
    private val datesFlow = MutableStateFlow<List<LocalDate>>(emptyList())

    fun setDatesWithEntries(dates: List<LocalDate>) {
        datesFlow.value = dates
    }

    fun setEntryForDate(date: LocalDate, entry: JournalEntry) {
        val updated = entriesFlow.value.toMutableMap()
        updated[date] = entry
        entriesFlow.value = updated
        datesFlow.value = updated.keys.toList()
    }

    override fun getEntryById(id: String): Flow<JournalEntry?> {
        return entriesFlow.map { map -> map.values.firstOrNull { it.id == id } }
    }

    override fun getEntryByDate(date: LocalDate): Flow<JournalEntry?> {
        return entriesFlow.map { map -> map[date] }
    }

    override suspend fun getEntryByDateSync(date: LocalDate): JournalEntry? {
        return entriesFlow.value[date]
    }

    override fun getEntriesInDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<JournalEntry>> {
        return entriesFlow.map { map -> map.values.filter { it.date in startDate..endDate } }
    }

    override fun getAllEntries(): Flow<List<JournalEntry>> {
        return entriesFlow.map { map -> map.values.toList() }
    }

    override fun getAllDatesWithEntries(): Flow<List<LocalDate>> {
        return datesFlow
    }

    override suspend fun saveEntry(entry: JournalEntry) {
        setEntryForDate(entry.date, entry)
    }

    override suspend fun updateEntry(entry: JournalEntry) {
        setEntryForDate(entry.date, entry)
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        val updated = entriesFlow.value.toMutableMap()
        updated.remove(entry.date)
        entriesFlow.value = updated
        datesFlow.value = updated.keys.toList()
    }

    override suspend fun deleteEntryById(id: String) {
        val updated = entriesFlow.value.toMutableMap()
        val entry = updated.values.firstOrNull { it.id == id }
        if (entry != null) {
            updated.remove(entry.date)
            entriesFlow.value = updated
            datesFlow.value = updated.keys.toList()
        }
    }
}
