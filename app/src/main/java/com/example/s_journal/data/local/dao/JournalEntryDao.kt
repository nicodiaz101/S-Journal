package com.example.s_journal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.s_journal.data.local.entity.JournalEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface JournalEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Update
    suspend fun updateEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntryById(id: String)

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    fun getEntryById(id: String): Flow<JournalEntry?>

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    fun getEntryByDate(date: LocalDate): Flow<JournalEntry?>

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryByDateSync(date: LocalDate): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getEntriesInDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Query("SELECT DISTINCT date FROM journal_entries")
    fun getAllDatesWithEntries(): Flow<List<LocalDate>>
}
