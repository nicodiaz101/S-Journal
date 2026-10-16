package com.example.s_journal.data.local.storage

import com.example.s_journal.ui.canvas.model.Stroke
import java.time.LocalDate

interface StrokeStorage {
    suspend fun loadStrokes(date: LocalDate): List<Stroke>
    suspend fun saveStrokes(date: LocalDate, strokes: List<Stroke>): String
    suspend fun deleteStrokes(date: LocalDate)
}
