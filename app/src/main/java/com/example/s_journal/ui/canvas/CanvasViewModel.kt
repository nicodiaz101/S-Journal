package com.example.s_journal.ui.canvas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.s_journal.SJournalApplication
import com.example.s_journal.data.local.entity.JournalEntry
import com.example.s_journal.data.model.EntryType
import com.example.s_journal.domain.repository.JournalRepository
import com.example.s_journal.ui.canvas.engine.StrokeSerializer
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.components.SpenTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate

data class CanvasUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val strokes: List<Stroke> = emptyList(),
    val currentTool: SpenTool = SpenTool.PEN,
    val currentColor: Int = android.graphics.Color.WHITE,
    val currentStrokeWidth: Float = 6f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showRuledLines: Boolean = true,
    val isSaving: Boolean = false
)

class CanvasViewModel(
    application: Application,
    private val journalRepository: JournalRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CanvasUiState())
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    private var saveDebounceJob: Job? = null

    init {
        loadStrokesForDate(LocalDate.now())
    }

    fun setDate(date: LocalDate) {
        if (_uiState.value.selectedDate != date) {
            _uiState.update { it.copy(selectedDate = date) }
            loadStrokesForDate(date)
        }
    }

    fun onStrokesChanged(newStrokes: List<Stroke>) {
        _uiState.update {
            it.copy(
                strokes = newStrokes,
                canUndo = newStrokes.isNotEmpty()
            )
        }

        // Debounced autosave
        saveDebounceJob?.cancel()
        saveDebounceJob = viewModelScope.launch {
            delay(500)
            saveStrokesToDiskAndDatabase(newStrokes, _uiState.value.selectedDate)
        }
    }

    fun onToolSelected(tool: SpenTool) {
        _uiState.update { it.copy(currentTool = tool) }
    }

    fun onColorSelected(color: Int) {
        _uiState.update { it.copy(currentColor = color) }
    }

    fun onStrokeWidthSelected(width: Float) {
        _uiState.update { it.copy(currentStrokeWidth = width) }
    }

    fun onToggleRuledLines() {
        _uiState.update { it.copy(showRuledLines = !it.showRuledLines) }
    }

    fun clearCurrentCanvas() {
        val date = _uiState.value.selectedDate
        _uiState.update { it.copy(strokes = emptyList(), canUndo = false, canRedo = false) }

        viewModelScope.launch(Dispatchers.IO) {
            val file = getStrokesFile(date)
            if (file.exists()) {
                file.delete()
            }
            journalRepository.deleteEntryById(date.toString())
        }
    }

    private fun loadStrokesForDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = getStrokesFile(date)
            val loadedStrokes = if (file.exists()) {
                try {
                    StrokeSerializer.loadFromFile(file)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        strokes = loadedStrokes,
                        canUndo = loadedStrokes.isNotEmpty(),
                        canRedo = false
                    )
                }
            }
        }
    }

    private suspend fun saveStrokesToDiskAndDatabase(strokes: List<Stroke>, date: LocalDate) {
        withContext(Dispatchers.IO) {
            val file = getStrokesFile(date)
            if (strokes.isEmpty()) {
                if (file.exists()) file.delete()
                journalRepository.deleteEntryById(date.toString())
            } else {
                StrokeSerializer.saveToFile(file, strokes)

                val existingEntry = journalRepository.getEntryByDateSync(date)
                val now = Instant.now()
                val entry = JournalEntry(
                    id = date.toString(),
                    date = date,
                    createdAt = existingEntry?.createdAt ?: now,
                    updatedAt = now,
                    type = EntryType.CANVAS,
                    title = "Handwritten Entry",
                    strokeDataPath = file.absolutePath
                )
                journalRepository.saveEntry(entry)
            }
        }
    }

    private fun getStrokesFile(date: LocalDate): File {
        val strokesDir = File(getApplication<Application>().filesDir, "strokes")
        return File(strokesDir, "$date.sjink")
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SJournalApplication)
                CanvasViewModel(application, application.repository)
            }
        }
    }
}
