package com.example.s_journal.ui.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.s_journal.data.local.entity.JournalEntry
import com.example.s_journal.ui.canvas.CanvasViewModel
import com.example.s_journal.ui.canvas.view.SpenCanvasComposable
import com.example.s_journal.ui.canvas.view.SpenCanvasView
import com.example.s_journal.ui.components.ColorPickerModal
import com.example.s_journal.ui.components.FullscreenToggleButton
import com.example.s_journal.ui.components.RuledCanvasBackground
import com.example.s_journal.ui.components.SpenToolbarPill
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EntryPanelScreen(
    selectedDate: LocalDate,
    entry: JournalEntry?,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    canvasViewModel: CanvasViewModel = viewModel(factory = CanvasViewModel.Factory)
) {
    val canvasUiState by canvasViewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    // Sync default color with theme on initial launch
    val defaultInkColor = remember(isDark) {
        if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
    }

    LaunchedEffect(isDark) {
        if (canvasUiState.currentColor == android.graphics.Color.WHITE && !isDark) {
            canvasViewModel.onColorSelected(android.graphics.Color.BLACK)
        } else if (canvasUiState.currentColor == android.graphics.Color.BLACK && isDark) {
            canvasViewModel.onColorSelected(android.graphics.Color.WHITE)
        }
    }

    LaunchedEffect(selectedDate) {
        canvasViewModel.setDate(selectedDate)
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) {
        canvasViewModel.flushPendingSave()
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var canvasViewInstance by remember { mutableStateOf<SpenCanvasView?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Ruled lines background
            AnimatedVisibility(
                visible = canvasUiState.showRuledLines,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                RuledCanvasBackground(
                    modifier = Modifier.fillMaxSize(),
                    lineSpacing = 36.dp,
                    topMargin = 64.dp
                )
            }

            // 2. Active S Pen Canvas (Live drawing & stroke rendering)
            SpenCanvasComposable(
                strokes = canvasUiState.strokes,
                currentTool = canvasUiState.currentTool,
                currentColor = canvasUiState.currentColor,
                currentStrokeWidth = canvasUiState.currentStrokeWidth,
                onStrokesChanged = { updatedStrokes, canUndo, canRedo ->
                    canvasViewModel.onStrokesChanged(updatedStrokes, canUndo, canRedo)
                },
                canvasViewRef = { view ->
                    canvasViewInstance = view
                },
                modifier = Modifier.fillMaxSize()
            )

            // 3. Watermark placeholder for empty days (non-interactive, fades out when strokes exist)
            if (canvasUiState.strokes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start writing with S Pen to create an entry",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 4. Floating Fullscreen Toggle Button [⛶] (Top Right)
            FullscreenToggleButton(
                isFullscreen = isFullscreen,
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )

            // 5. Floating S Pen Toolbar Pill (Bottom Center)
            SpenToolbarPill(
                selectedTool = canvasUiState.currentTool,
                currentColor = canvasUiState.currentColor,
                canUndo = canvasUiState.canUndo,
                canRedo = canvasUiState.canRedo,
                onToolSelected = { tool -> canvasViewModel.onToolSelected(tool) },
                onUndo = {
                    canvasViewInstance?.undo()
                },
                onRedo = {
                    canvasViewInstance?.redo()
                },
                onColorPickerClick = { showColorPicker = true },
                onToggleRuledLines = { canvasViewModel.onToggleRuledLines() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )

            // 6. Color and Thickness Modal Dialog
            if (showColorPicker) {
                ColorPickerModal(
                    selectedColor = canvasUiState.currentColor,
                    selectedWidth = canvasUiState.currentStrokeWidth,
                    onColorSelected = { color ->
                        canvasViewModel.onColorSelected(color)
                    },
                    onWidthSelected = { width ->
                        canvasViewModel.onStrokeWidthSelected(width)
                    },
                    onDismissRequest = { showColorPicker = false }
                )
            }
        }
    }
}
