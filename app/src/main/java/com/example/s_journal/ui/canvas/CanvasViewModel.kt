package com.example.s_journal.ui.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s_journal.data.local.entity.JournalEntry
import com.example.s_journal.data.local.storage.StrokeStorage
import com.example.s_journal.data.model.EntryType
import com.example.s_journal.di.IoDispatcher
import com.example.s_journal.domain.repository.JournalRepository
import com.example.s_journal.ui.canvas.model.SpenTool
import com.example.s_journal.ui.canvas.model.Stroke
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class CanvasUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val strokes: List<Stroke> = emptyList(),
    val currentTool: SpenTool = SpenTool.PEN,
    val currentColor: Int = android.graphics.Color.WHITE,
    val currentStrokeWidth: Float = 6f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showRuledLines: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CanvasViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val strokeStorage: StrokeStorage,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CanvasUiState(selectedDate = LocalDate.now(clock)))
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    private var saveDebounceJob: Job? = null
    private var loadJob: Job? = null
    private var pendingSaveStrokes: List<Stroke>? = null
    private var pendingSaveDate: LocalDate? = null

    init {
        loadStrokesForDate(LocalDate.now(clock))
    }

    fun setDate(date: LocalDate) {
        val oldDate = _uiState.value.selectedDate
        if (oldDate != date) {
            // Flush any pending save for the previous date immediately before switching
            flushPendingSaveSync()
            loadJob?.cancel()
            _uiState.update { it.copy(selectedDate = date) }
            loadStrokesForDate(date)
        }
    }

    fun onStrokesChanged(newStrokes: List<Stroke>, canUndo: Boolean = newStrokes.isNotEmpty(), canRedo: Boolean = false) {
        val targetDate = _uiState.value.selectedDate
        pendingSaveStrokes = newStrokes
        pendingSaveDate = targetDate

        _uiState.update {
            it.copy(
                strokes = newStrokes,
                canUndo = canUndo,
                canRedo = canRedo
            )
        }

        // Debounced autosave capturing the exact date when strokes were drawn
        saveDebounceJob?.cancel()
        saveDebounceJob = viewModelScope.launch {
            delay(500)
            saveStrokesToDiskAndDatabase(newStrokes, targetDate)
            if (pendingSaveDate == targetDate) {
                pendingSaveStrokes = null
                pendingSaveDate = null
            }
        }
    }

    fun flushPendingSave() {
        val strokes = pendingSaveStrokes
        val date = pendingSaveDate
        if (strokes != null && date != null) {
            saveDebounceJob?.cancel()
            pendingSaveStrokes = null
            pendingSaveDate = null
            viewModelScope.launch(ioDispatcher) {
                saveStrokesToDiskAndDatabase(strokes, date)
            }
        }
    }

    private fun flushPendingSaveSync() {
        val strokes = pendingSaveStrokes
        val date = pendingSaveDate
        if (strokes != null && date != null) {
            saveDebounceJob?.cancel()
            pendingSaveStrokes = null
            pendingSaveDate = null
            // Save immediately in background without debounce delay
            viewModelScope.launch(ioDispatcher) {
                saveStrokesToDiskAndDatabase(strokes, date)
            }
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
        saveDebounceJob?.cancel()
        pendingSaveStrokes = null
        pendingSaveDate = null
        _uiState.update { it.copy(strokes = emptyList(), canUndo = false, canRedo = false) }

        viewModelScope.launch(ioDispatcher) {
            try {
                strokeStorage.deleteStrokes(date)
                journalRepository.deleteEntryById(date.toString())
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                android.util.Log.e("SJournal", "Failed to clear canvas for $date", e)
                _uiState.update { it.copy(errorMessage = "Failed to clear canvas: ${e.localizedMessage}") }
            }
        }
    }

    private fun loadStrokesForDate(date: LocalDate) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(ioDispatcher) {
            var loadError: String? = null
            val loadedStrokes = try {
                strokeStorage.loadStrokes(date)
            } catch (e: Exception) {
                android.util.Log.e("SJournal", "Failed to load strokes for $date", e)
                loadError = "Failed to load strokes: ${e.localizedMessage}"
                emptyList()
            }

            // Ensure date has not changed while loading before updating state
            if (_uiState.value.selectedDate == date) {
                _uiState.update {
                    it.copy(
                        strokes = loadedStrokes,
                        canUndo = false,
                        canRedo = false,
                        errorMessage = loadError
                    )
                }
            }
        }
    }

    private suspend fun saveStrokesToDiskAndDatabase(strokes: List<Stroke>, date: LocalDate) {
        withContext(ioDispatcher) {
            _uiState.update { it.copy(isSaving = true) }
            try {
                if (strokes.isEmpty()) {
                    strokeStorage.deleteStrokes(date)
                    journalRepository.deleteEntryById(date.toString())
                } else {
                    val relativePath = strokeStorage.saveStrokes(date, strokes)
                    val existingEntry = journalRepository.getEntryByDateSync(date)
                    val now = Instant.now(clock)
                    val entry = JournalEntry(
                        id = date.toString(),
                        date = date,
                        createdAt = existingEntry?.createdAt ?: now,
                        updatedAt = now,
                        type = EntryType.CANVAS,
                        title = "Handwritten Entry",
                        strokeDataPath = relativePath
                    )
                    journalRepository.saveEntry(entry)
                }
                _uiState.update { it.copy(isSaving = false, errorMessage = null) }
            } catch (e: Exception) {
                android.util.Log.e("SJournal", "Failed to save strokes for date $date", e)
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to save entry: ${e.localizedMessage}") }
            }
        }
    }
}
