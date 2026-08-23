package com.example.s_journal.ui.canvas.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.s_journal.ui.canvas.model.Stroke
import com.example.s_journal.ui.components.SpenTool

@Composable
fun SpenCanvasComposable(
    strokes: List<Stroke>,
    currentTool: SpenTool,
    currentColor: Int,
    currentStrokeWidth: Float,
    onStrokesChanged: (List<Stroke>) -> Unit,
    modifier: Modifier = Modifier,
    canvasViewRef: ((SpenCanvasView) -> Unit)? = null
) {
    val context = LocalContext.current
    val canvasView = remember {
        SpenCanvasView(context).apply {
            canvasViewRef?.invoke(this)
        }
    }

    // Sync state into canvasView
    LaunchedEffect(currentTool) {
        canvasView.currentTool = currentTool
    }

    LaunchedEffect(currentColor) {
        canvasView.currentColor = currentColor
    }

    LaunchedEffect(currentStrokeWidth) {
        canvasView.currentStrokeWidth = currentStrokeWidth
    }

    LaunchedEffect(strokes) {
        if (canvasView.strokeBuffer.strokes != strokes) {
            canvasView.setStrokes(strokes)
        }
    }

    AndroidView(
        factory = {
            canvasView.apply {
                onStrokesChangedListener = {
                    onStrokesChanged(strokeBuffer.strokes)
                }
            }
        },
        update = { view ->
            view.currentTool = currentTool
            view.currentColor = currentColor
            view.currentStrokeWidth = currentStrokeWidth
            if (view.strokeBuffer.strokes != strokes) {
                view.setStrokes(strokes)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
