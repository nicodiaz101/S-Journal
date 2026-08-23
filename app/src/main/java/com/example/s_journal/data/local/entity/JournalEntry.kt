package com.example.s_journal.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.s_journal.data.model.EntryType
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "date")
    val date: LocalDate,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,

    @ColumnInfo(name = "type")
    val type: EntryType,

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "text_content")
    val textContent: String? = null,

    @ColumnInfo(name = "word_count")
    val wordCount: Int = 0,

    @ColumnInfo(name = "stroke_data_path")
    val strokeDataPath: String? = null,

    @ColumnInfo(name = "preview_thumbnail_path")
    val previewThumbnailPath: String? = null,

    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "mood")
    val mood: Int? = null
)
