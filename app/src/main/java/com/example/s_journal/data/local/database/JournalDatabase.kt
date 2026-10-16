package com.example.s_journal.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.s_journal.data.local.dao.JournalEntryDao
import com.example.s_journal.data.local.entity.JournalEntry

@Database(
    entities = [JournalEntry::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        const val DATABASE_NAME = "s_journal.db"
    }
}
