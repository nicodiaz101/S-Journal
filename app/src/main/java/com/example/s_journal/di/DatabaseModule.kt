package com.example.s_journal.di

import android.content.Context
import androidx.room.Room
import com.example.s_journal.data.local.dao.JournalEntryDao
import com.example.s_journal.data.local.database.JournalDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJournalDatabase(
        @ApplicationContext context: Context
    ): JournalDatabase {
        return Room.databaseBuilder(
            context,
            JournalDatabase::class.java,
            JournalDatabase.DATABASE_NAME
        ).build()

    }

    @Provides
    @Singleton
    fun provideJournalEntryDao(database: JournalDatabase): JournalEntryDao {
        return database.journalEntryDao()
    }
}
