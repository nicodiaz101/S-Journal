package com.example.s_journal.data.repository

import com.example.s_journal.data.local.dao.JournalEntryDao
import com.example.s_journal.data.local.entity.JournalEntry
import com.example.s_journal.data.model.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class JournalRepositoryTest {

    private lateinit var fakeDao: FakeJournalEntryDao
    private lateinit var repository: JournalRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeJournalEntryDao()
        repository = JournalRepositoryImpl(fakeDao)
    }

    @Test
    fun saveEntry_and_getEntryByDate_returnsCorrectEntry() = runTest {
        val date = LocalDate.of(2026, 8, 23)
        val entry = JournalEntry(
            id = "test-1",
            date = date,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            type = EntryType.TEXT,
            title = "Mi Diario",
            textContent = "Hoy fue un día productivo."
        )

        repository.saveEntry(entry)

        val retrieved = repository.getEntryByDate(date).first()
        assertNotNull(retrieved)
        assertEquals("test-1", retrieved?.id)
        assertEquals("Mi Diario", retrieved?.title)
        assertEquals("Hoy fue un día productivo.", retrieved?.textContent)
    }

    @Test
    fun deleteEntry_removesEntry() = runTest {
        val date = LocalDate.of(2026, 8, 23)
        val entry = JournalEntry(
            id = "test-2",
            date = date,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            type = EntryType.CANVAS,
            title = "Dibujo S Pen"
        )

        repository.saveEntry(entry)
        repository.deleteEntry(entry)

        val retrieved = repository.getEntryByDate(date).first()
        assertNull(retrieved)
    }

    @Test
    fun getAllDatesWithEntries_returnsUniqueDates() = runTest {
        val date1 = LocalDate.of(2026, 8, 20)
        val date2 = LocalDate.of(2026, 8, 21)

        repository.saveEntry(
            JournalEntry(
                id = "1",
                date = date1,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                type = EntryType.TEXT
            )
        )
        repository.saveEntry(
            JournalEntry(
                id = "2",
                date = date2,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                type = EntryType.CANVAS
            )
        )

        val dates = repository.getAllDatesWithEntries().first()
        assertEquals(2, dates.size)
        assert(dates.contains(date1))
        assert(dates.contains(date2))
    }
}

private class FakeJournalEntryDao : JournalEntryDao {
    private val entries = mutableMapOf<String, JournalEntry>()

    override suspend fun insertEntry(entry: JournalEntry) {
        entries[entry.id] = entry
    }

    override suspend fun updateEntry(entry: JournalEntry) {
        entries[entry.id] = entry
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        entries.remove(entry.id)
    }

    override suspend fun deleteEntryById(id: String) {
        entries.remove(id)
    }

    override fun getEntryById(id: String): Flow<JournalEntry?> {
        return flowOf(entries[id])
    }

    override fun getEntryByDate(date: LocalDate): Flow<JournalEntry?> {
        return flowOf(entries.values.firstOrNull { it.date == date })
    }

    override suspend fun getEntryByDateSync(date: LocalDate): JournalEntry? {
        return entries.values.firstOrNull { it.date == date }
    }

    override fun getEntriesInDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<JournalEntry>> {
        return flowOf(entries.values.filter { it.date in startDate..endDate })
    }

    override fun getAllEntries(): Flow<List<JournalEntry>> {
        return flowOf(entries.values.toList())
    }

    override fun getAllDatesWithEntries(): Flow<List<LocalDate>> {
        return flowOf(entries.values.map { it.date }.distinct())
    }
}
