package com.example.s_journal.domain.repository

import com.example.s_journal.data.local.entity.JournalEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface JournalRepository {
    fun getEntryById(id: String): Flow<JournalEntry?>
    fun getEntryByDate(date: LocalDate): Flow<JournalEntry?>
    suspend fun getEntryByDateSync(date: LocalDate): JournalEntry?
    fun getEntriesInDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<JournalEntry>>
    fun getAllEntries(): Flow<List<JournalEntry>>
    fun getAllDatesWithEntries(): Flow<List<LocalDate>>
    suspend fun saveEntry(entry: JournalEntry)
    suspend fun updateEntry(entry: JournalEntry)
    suspend fun deleteEntry(entry: JournalEntry)
    suspend fun deleteEntryById(id: String)
}
