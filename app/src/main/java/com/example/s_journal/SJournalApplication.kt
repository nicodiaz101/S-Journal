package com.example.s_journal

import android.app.Application
import androidx.room.Room
import com.example.s_journal.data.local.database.JournalDatabase
import com.example.s_journal.data.repository.JournalRepositoryImpl
import com.example.s_journal.domain.repository.JournalRepository

open class SJournalApplication : Application() {
    lateinit var database: JournalDatabase
        private set
    lateinit var repository: JournalRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            JournalDatabase::class.java,
            JournalDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(true).build()
        repository = JournalRepositoryImpl(database.journalEntryDao())
    }
}

