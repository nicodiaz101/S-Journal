package com.example.s_journal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.s_journal.theme.RuledLineColorDark
import com.example.s_journal.theme.RuledLineColorLight

@Composable
fun RuledCanvasBackground(
    modifier: Modifier = Modifier,
    lineSpacing: Dp = 32.dp,
    topMargin: Dp = 48.dp,
    lineColor: Color = if (isSystemInDarkTheme()) RuledLineColorDark else RuledLineColorLight,
    strokeWidth: Dp = 1.dp
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = lineSpacing.toPx()
        val topMarginPx = topMargin.toPx()
        val strokeWidthPx = strokeWidth.toPx()

        var currentY = topMarginPx
        while (currentY < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(x = 0f, y = currentY),
                end = Offset(x = size.width, y = currentY),
                strokeWidth = strokeWidthPx
            )
            currentY += spacingPx
        }
    }
}
