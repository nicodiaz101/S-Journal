package com.example.s_journal.data.repository

import com.example.s_journal.data.local.dao.JournalEntryDao
import com.example.s_journal.data.local.entity.JournalEntry
import com.example.s_journal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val journalEntryDao: JournalEntryDao
) : JournalRepository {

    override fun getEntryById(id: String): Flow<JournalEntry?> {
        return journalEntryDao.getEntryById(id)
    }

    override fun getEntryByDate(date: LocalDate): Flow<JournalEntry?> {
        return journalEntryDao.getEntryByDate(date)
    }

    override fun getEntriesInDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<JournalEntry>> {
        return journalEntryDao.getEntriesInDateRange(startDate, endDate)
    }

    override fun getAllEntries(): Flow<List<JournalEntry>> {
        return journalEntryDao.getAllEntries()
    }

    override fun getAllDatesWithEntries(): Flow<List<LocalDate>> {
        return journalEntryDao.getAllDatesWithEntries()
    }

    override suspend fun getEntryByDateSync(date: LocalDate): JournalEntry? {
        return journalEntryDao.getEntryByDateSync(date)
    }

    override suspend fun saveEntry(entry: JournalEntry) {
        journalEntryDao.insertEntry(entry)
    }

    override suspend fun updateEntry(entry: JournalEntry) {
        journalEntryDao.updateEntry(entry)
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        journalEntryDao.deleteEntry(entry)
    }

    override suspend fun deleteEntryById(id: String) {
        journalEntryDao.deleteEntryById(id)
    }
}
