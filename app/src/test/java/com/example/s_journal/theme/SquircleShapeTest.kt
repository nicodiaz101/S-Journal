package com.example.s_journal.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SquircleShapeTest {

    private val density = Density(density = 2f)


    @Test
    fun createOutline_withZeroSize_returnsOutlineSafely() {
        val shape = SquircleShape(radius = 16.dp)
        val outline = shape.createOutline(
            size = Size.Zero,
            layoutDirection = LayoutDirection.Ltr,
            density = density
        )

        assertTrue(outline is Outline.Generic)
    }

    @Test
    fun squircleShape_asymmetricCorners_createsDistinctOutline() {
        val asymmetric = SquircleShape(topEnd = 24.dp, bottomEnd = 24.dp)
        val symmetric = SquircleShape(radius = 24.dp)

        assertNotEquals(asymmetric, symmetric)
        assertEquals(0f, asymmetric.topStart.toPx(Size(200f, 200f), density), 0.001f)
        assertEquals(48f, asymmetric.topEnd.toPx(Size(200f, 200f), density), 0.001f)
        assertEquals(48f, asymmetric.bottomEnd.toPx(Size(200f, 200f), density), 0.001f)
        assertEquals(0f, asymmetric.bottomStart.toPx(Size(200f, 200f), density), 0.001f)
    }

    @Test
    fun squircleShape_equalityAndHashCode_workProperly() {
        val shape1 = SquircleShape(radius = 28.dp, smoothing = 0.72f)
        val shape2 = SquircleShape(radius = 28.dp, smoothing = 0.72f)
        val shape3 = SquircleShape(radius = 20.dp, smoothing = 0.72f)

        assertEquals(shape1, shape2)
        assertEquals(shape1.hashCode(), shape2.hashCode())
        assertNotEquals(shape1, shape3)
    }

    @Test
    fun squircleShape_copy_preservesSmoothing() {
        val shape = SquircleShape(radius = 28.dp, smoothing = 0.85f)
        val copy = shape.copy(topStart = CornerSize(12.dp))

        assertTrue(copy is SquircleShape)
        assertEquals(0.85f, (copy as SquircleShape).smoothing, 0.0001f)
    }
}
