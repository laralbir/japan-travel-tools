package com.laralbir.japantraveltools.feature.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomPanStateTest {

    @Test
    fun `initial state is at 1x scale, zero offset, not zoomed`() {
        val state = ZoomPanState()
        assertEquals(1.0f, state.scale, 0.001f)
        assertEquals(0.0f, state.offsetX, 0.001f)
        assertEquals(0.0f, state.offsetY, 0.001f)
        assertFalse(state.isZoomed)
        assertEquals(100, state.zoomPercentage)
    }

    @Test
    fun `onTransform increases scale and clamps at maxScale 5x`() {
        val state = ZoomPanState()
        state.onTransform(zoomDelta = 2.0f, panDeltaX = 0f, panDeltaY = 0f, containerWidth = 1000f, containerHeight = 1000f)
        assertEquals(2.0f, state.scale, 0.001f)
        assertTrue(state.isZoomed)
        assertEquals(200, state.zoomPercentage)

        // Multiply by 4 (would be 8x, but clamped to 5x)
        state.onTransform(zoomDelta = 4.0f, panDeltaX = 0f, panDeltaY = 0f, containerWidth = 1000f, containerHeight = 1000f)
        assertEquals(5.0f, state.scale, 0.001f)
        assertEquals(500, state.zoomPercentage)
    }

    @Test
    fun `onTransform decreases scale and clamps at minScale 1x`() {
        val state = ZoomPanState(initialScale = 2.0f)
        state.onTransform(zoomDelta = 0.2f, panDeltaX = 0f, panDeltaY = 0f, containerWidth = 1000f, containerHeight = 1000f)
        assertEquals(1.0f, state.scale, 0.001f)
        assertFalse(state.isZoomed)
        assertEquals(0.0f, state.offsetX, 0.001f)
        assertEquals(0.0f, state.offsetY, 0.001f)
    }

    @Test
    fun `panning when scale is 1x does not move offset`() {
        val state = ZoomPanState()
        state.onTransform(zoomDelta = 1.0f, panDeltaX = 50f, panDeltaY = -50f, containerWidth = 1000f, containerHeight = 1000f)
        assertEquals(0.0f, state.offsetX, 0.001f)
        assertEquals(0.0f, state.offsetY, 0.001f)
    }

    @Test
    fun `panning when zoomed translates offset within bounds`() {
        val state = ZoomPanState(initialScale = 2.0f) // container 1000x1000, scale 2x -> maxOffset = 500
        state.onTransform(zoomDelta = 1.0f, panDeltaX = 100f, panDeltaY = -150f, containerWidth = 1000f, containerHeight = 1000f)
        assertEquals(100.0f, state.offsetX, 0.001f)
        assertEquals(-150.0f, state.offsetY, 0.001f)

        // Pan beyond max offset: maxOffset = (1000 * (2 - 1)) / 2 = 500
        state.onTransform(zoomDelta = 1.0f, panDeltaX = 1000f, panDeltaY = -1000f, containerWidth = 1000f, containerHeight = 1000f)
        assertEquals(500.0f, state.offsetX, 0.001f)
        assertEquals(-500.0f, state.offsetY, 0.001f)
    }

    @Test
    fun `toggleDoubleTapZoom alternates between 1x and target scale`() {
        val state = ZoomPanState()
        assertFalse(state.isZoomed)

        // First double tap: zoom to 2.5x
        state.toggleDoubleTapZoom(targetScale = 2.5f)
        assertEquals(2.5f, state.scale, 0.001f)
        assertTrue(state.isZoomed)
        assertEquals(250, state.zoomPercentage)

        // Second double tap: reset to 1x
        state.toggleDoubleTapZoom()
        assertEquals(1.0f, state.scale, 0.001f)
        assertFalse(state.isZoomed)
        assertEquals(0.0f, state.offsetX, 0.001f)
        assertEquals(0.0f, state.offsetY, 0.001f)
    }

    @Test
    fun `reset restores scale to 1x and offset to zero`() {
        val state = ZoomPanState(initialScale = 3.0f, initialOffsetX = 120f, initialOffsetY = -80f)
        assertTrue(state.isZoomed)

        state.reset()
        assertEquals(1.0f, state.scale, 0.001f)
        assertEquals(0.0f, state.offsetX, 0.001f)
        assertEquals(0.0f, state.offsetY, 0.001f)
        assertFalse(state.isZoomed)
    }
}
