package com.example.s_journal.data.local.storage

import android.content.Context
import android.util.Log
import com.example.s_journal.ui.canvas.engine.StrokeSerializer
import com.example.s_journal.ui.canvas.model.Stroke
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrokeStorageImpl private constructor(
    private val filesDirProvider: () -> File
) : StrokeStorage {

    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this({ context.filesDir })

    internal constructor(filesDir: File) : this({ filesDir })

    private fun getStrokesFile(date: LocalDate): File {
        val strokesDir = File(filesDirProvider(), "strokes")
        return File(strokesDir, "$date.sjink")
    }

    override suspend fun loadStrokes(date: LocalDate): List<Stroke> {
        val file = getStrokesFile(date)
        if (!file.exists()) return emptyList()

        return try {
            StrokeSerializer.loadFromFile(file)
        } catch (e: Exception) {
            Log.e("SJournal", "Corrupt or unreadable .sjink file for $date. Preserving as .corrupt", e)
            try {
                val corruptFile = File(file.parentFile, "${file.name}.corrupt")
                file.renameTo(corruptFile)
            } catch (ignored: Exception) {}
            emptyList()
        }
    }

    override suspend fun saveStrokes(date: LocalDate, strokes: List<Stroke>): String {
        val file = getStrokesFile(date)
        if (strokes.isEmpty()) {
            if (file.exists()) file.delete()
            return ""
        }
        StrokeSerializer.saveToFile(file, strokes)
        return "strokes/$date.sjink"
    }

    override suspend fun deleteStrokes(date: LocalDate) {
        val file = getStrokesFile(date)
        if (file.exists()) {
            file.delete()
        }
    }
}
