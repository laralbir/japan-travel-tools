package com.laralbir.japantraveltools.feature.documents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class ZoomPanState(
    initialScale: Float = 1.0f,
    initialOffsetX: Float = 0.0f,
    initialOffsetY: Float = 0.0f,
    val minScale: Float = 1.0f,
    val maxScale: Float = 5.0f
) {
    var scale: Float by mutableFloatStateOf(initialScale.coerceIn(minScale, maxScale))
    var offsetX: Float by mutableFloatStateOf(initialOffsetX)
    var offsetY: Float by mutableFloatStateOf(initialOffsetY)

    val isZoomed: Boolean get() = scale > 1.05f

    val zoomPercentage: Int get() = (scale * 100).toInt()

    fun onTransform(
        zoomDelta: Float,
        panDeltaX: Float,
        panDeltaY: Float,
        containerWidth: Float,
        containerHeight: Float
    ) {
        val newScale = (scale * zoomDelta).coerceIn(minScale, maxScale)
        scale = newScale

        if (newScale > 1.0f) {
            val maxOffsetX = maxOf(0f, (containerWidth * (newScale - 1f)) / 2f)
            val maxOffsetY = maxOf(0f, (containerHeight * (newScale - 1f)) / 2f)
            offsetX = (offsetX + panDeltaX).coerceIn(-maxOffsetX, maxOffsetX)
            offsetY = (offsetY + panDeltaY).coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    fun toggleDoubleTapZoom(targetScale: Float = 2.5f) {
        if (isZoomed) {
            reset()
        } else {
            scale = targetScale.coerceIn(minScale, maxScale)
            offsetX = 0f
            offsetY = 0f
        }
    }

    fun reset() {
        scale = 1.0f
        offsetX = 0f
        offsetY = 0f
    }
}

@Composable
fun rememberZoomPanState(): ZoomPanState {
    return remember { ZoomPanState() }
}
