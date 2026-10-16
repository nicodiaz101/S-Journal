package com.example.s_journal.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Superellipse / Squircle CornerBasedShape for Samsung One UI continuous curvature (G2-like continuity).
 * Inherits from CornerBasedShape to integrate natively into MaterialTheme.shapes and Compose surfaces.
 * Unlike standard RoundedCornerShape which creates sharp curvature transitions (circular arcs),
 * SquircleShape utilizes cubic Bézier curves to produce smooth, organic corners characteristic
 * of Samsung One UI cards, panels, and icon containers.
 */
class SquircleShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    val smoothing: Float = 0.72f // One UI superellipse curvature factor
) : CornerBasedShape(
    topStart = topStart,
    topEnd = topEnd,
    bottomEnd = bottomEnd,
    bottomStart = bottomStart
) {

    constructor(
        topStart: Dp = 0.dp,
        topEnd: Dp = 0.dp,
        bottomEnd: Dp = 0.dp,
        bottomStart: Dp = 0.dp,
        smoothing: Float = 0.72f
    ) : this(
        topStart = CornerSize(topStart),
        topEnd = CornerSize(topEnd),
        bottomEnd = CornerSize(bottomEnd),
        bottomStart = CornerSize(bottomStart),
        smoothing = smoothing
    )

    constructor(radius: Dp, smoothing: Float = 0.72f) : this(
        topStart = CornerSize(radius),
        topEnd = CornerSize(radius),
        bottomEnd = CornerSize(radius),
        bottomStart = CornerSize(radius),
        smoothing = smoothing
    )

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
    ): CornerBasedShape = SquircleShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
        smoothing = smoothing
    )

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        val width = size.width
        val height = size.height

        if (width <= 0f || height <= 0f) {
            return Outline.Generic(Path())
        }

        val maxRadius = minOf(width, height) / 2f
        val rTopStart = topStart.coerceAtMost(maxRadius)
        val rTopEnd = topEnd.coerceAtMost(maxRadius)
        val rBottomEnd = bottomEnd.coerceAtMost(maxRadius)
        val rBottomStart = bottomStart.coerceAtMost(maxRadius)

        val (resolvedTopStart, resolvedTopEnd) = if (layoutDirection == LayoutDirection.Rtl) {
            rTopEnd to rTopStart
        } else {
            rTopStart to rTopEnd
        }
        val (resolvedBottomStart, resolvedBottomEnd) = if (layoutDirection == LayoutDirection.Rtl) {
            rBottomEnd to rBottomStart
        } else {
            rBottomStart to rBottomEnd
        }

        val path = Path().apply {
            moveTo(resolvedTopStart, 0f)

            // Top edge & Top-Right corner
            lineTo(width - resolvedTopEnd, 0f)
            if (resolvedTopEnd > 0f) {
                val k = resolvedTopEnd * (1f - smoothing)
                cubicTo(
                    width - k, 0f,
                    width, k,
                    width, resolvedTopEnd
                )
            }

            // Right edge & Bottom-Right corner
            lineTo(width, height - resolvedBottomEnd)
            if (resolvedBottomEnd > 0f) {
                val k = resolvedBottomEnd * (1f - smoothing)
                cubicTo(
                    width, height - k,
                    width - k, height,
                    width - resolvedBottomEnd, height
                )
            }

            // Bottom edge & Bottom-Left corner
            lineTo(resolvedBottomStart, height)
            if (resolvedBottomStart > 0f) {
                val k = resolvedBottomStart * (1f - smoothing)
                cubicTo(
                    k, height,
                    0f, height - k,
                    0f, height - resolvedBottomStart
                )
            }

            // Left edge & Top-Left corner
            lineTo(0f, resolvedTopStart)
            if (resolvedTopStart > 0f) {
                val k = resolvedTopStart * (1f - smoothing)
                cubicTo(
                    0f, k,
                    k, 0f,
                    resolvedTopStart, 0f
                )
            }

            close()
        }

        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquircleShape) return false
        return topStart == other.topStart &&
                topEnd == other.topEnd &&
                bottomEnd == other.bottomEnd &&
                bottomStart == other.bottomStart &&
                smoothing == other.smoothing
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + smoothing.hashCode()
        return result
    }
}
