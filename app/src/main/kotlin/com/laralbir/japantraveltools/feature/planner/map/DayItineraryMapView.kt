package com.laralbir.japantraveltools.feature.planner.map

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import java.util.Locale
import kotlin.math.max
import kotlin.math.sqrt

@Composable
fun DayItineraryMapView(
    stops: List<ItineraryStopItem>,
    selectedStop: ItineraryStopItem?,
    onStopClick: (ItineraryStopItem) -> Unit,
    userLocation: GeoPoint?,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onOpenExternalRoute: () -> Unit,
    isEs: Boolean,
    modifier: Modifier = Modifier
) {
    val validStops = remember(stops) {
        stops.filter { it.latitude != 0.0 && it.longitude != 0.0 }
    }

    if (validStops.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🗺️", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isEs) "No hay paradas con coordenadas para este día"
                        else "No stops with coordinates for this day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isEs) "Edita o añade visitas con coordenadas geográficas para ver la ruta y el mapa interactivo."
                        else "Edit or add stops with geographic coordinates to see the interactive route.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    val boundingBox = remember(validStops) {
        val points = validStops.map { GeoPoint(it.latitude, it.longitude) }
        GeoProjectionUtils.calculateBoundingBox(points, paddingRatio = 0.25)
    }

    val totalRouteKm = remember(validStops) {
        GeoProjectionUtils.calculateRouteTotalDistanceKm(validStops)
    }

    // Gestures state
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    val density = LocalDensity.current
    val markerRadiusPx = with(density) { 18.dp.toPx() }
    val selectedMarkerRadiusPx = with(density) { 24.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        // Main Interactive Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        zoomScale = (zoomScale * zoomChange).coerceIn(0.5f, 10.0f)
                        panOffset += pan
                    }
                }
                .pointerInput(validStops, zoomScale, panOffset) {
                    detectTapGestures { tapOffset ->
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()

                        val (minNormX, minNormY) = GeoProjectionUtils.projectToMercatorNormalized(
                            boundingBox.maxLat,
                            boundingBox.minLng
                        )
                        val (maxNormX, maxNormY) = GeoProjectionUtils.projectToMercatorNormalized(
                            boundingBox.minLat,
                            boundingBox.maxLng
                        )

                        val spanNormX = max(maxNormX - minNormX, 0.00001)
                        val spanNormY = max(maxNormY - minNormY, 0.00001)

                        val centerX = canvasWidth / 2f
                        val centerY = canvasHeight / 2f

                        // Find closest stop to tap
                        val hitStop = validStops.minByOrNull { stop ->
                            val (nx, ny) = GeoProjectionUtils.projectToMercatorNormalized(stop.latitude, stop.longitude)
                            val basePxX = ((nx - minNormX) / spanNormX).toFloat() * (canvasWidth * 0.74f) + (canvasWidth * 0.13f)
                            val basePxY = ((ny - minNormY) / spanNormY).toFloat() * (canvasHeight * 0.74f) + (canvasHeight * 0.13f)

                            val sx = (basePxX - centerX) * zoomScale + centerX + panOffset.x
                            val sy = (basePxY - centerY) * zoomScale + centerY + panOffset.y

                            val dx = sx - tapOffset.x
                            val dy = sy - tapOffset.y
                            sqrt(dx * dx + dy * dy)
                        }

                        if (hitStop != null) {
                            val (nx, ny) = GeoProjectionUtils.projectToMercatorNormalized(hitStop.latitude, hitStop.longitude)
                            val basePxX = ((nx - minNormX) / spanNormX).toFloat() * (canvasWidth * 0.74f) + (canvasWidth * 0.13f)
                            val basePxY = ((ny - minNormY) / spanNormY).toFloat() * (canvasHeight * 0.74f) + (canvasHeight * 0.13f)
                            val sx = (basePxX - centerX) * zoomScale + centerX + panOffset.x
                            val sy = (basePxY - centerY) * zoomScale + centerY + panOffset.y

                            val distanceToTap = sqrt((sx - tapOffset.x) * (sx - tapOffset.x) + (sy - tapOffset.y) * (sy - tapOffset.y))
                            if (distanceToTap <= with(density) { 36.dp.toPx() }) {
                                onStopClick(hitStop)
                            }
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            val (minNormX, minNormY) = GeoProjectionUtils.projectToMercatorNormalized(
                boundingBox.maxLat,
                boundingBox.minLng
            )
            val (maxNormX, maxNormY) = GeoProjectionUtils.projectToMercatorNormalized(
                boundingBox.minLat,
                boundingBox.maxLng
            )

            val spanNormX = max(maxNormX - minNormX, 0.00001)
            val spanNormY = max(maxNormY - minNormY, 0.00001)

            val centerCanvasX = canvasW / 2f
            val centerCanvasY = canvasH / 2f

            fun toScreenCoords(lat: Double, lng: Double): Offset {
                val (nx, ny) = GeoProjectionUtils.projectToMercatorNormalized(lat, lng)
                val basePxX = ((nx - minNormX) / spanNormX).toFloat() * (canvasW * 0.74f) + (canvasW * 0.13f)
                val basePxY = ((ny - minNormY) / spanNormY).toFloat() * (canvasH * 0.74f) + (canvasH * 0.13f)

                val sx = (basePxX - centerCanvasX) * zoomScale + centerCanvasX + panOffset.x
                val sy = (basePxY - centerCanvasY) * zoomScale + centerCanvasY + panOffset.y
                return Offset(sx, sy)
            }

            // 1. Draw subtle background coordinate grid
            val gridStep = 60f * zoomScale
            var gx = (panOffset.x % gridStep)
            while (gx < canvasW) {
                drawLine(
                    color = gridColor,
                    start = Offset(gx, 0f),
                    end = Offset(gx, canvasH),
                    strokeWidth = 1f
                )
                gx += gridStep
            }
            var gy = (panOffset.y % gridStep)
            while (gy < canvasH) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, gy),
                    end = Offset(canvasW, gy),
                    strokeWidth = 1f
                )
                gy += gridStep
            }

            // 2. Draw Polyline route between stops
            if (validStops.size >= 2) {
                val screenPoints = validStops.map { toScreenCoords(it.latitude, it.longitude) }

                // Outer route glow
                for (i in 0 until screenPoints.size - 1) {
                    val p1 = screenPoints[i]
                    val p2 = screenPoints[i + 1]
                    drawLine(
                        color = primaryColor.copy(alpha = 0.25f),
                        start = p1,
                        end = p2,
                        strokeWidth = with(density) { 8.dp.toPx() },
                        cap = StrokeCap.Round
                    )
                }

                // Inner crisp route line
                for (i in 0 until screenPoints.size - 1) {
                    val p1 = screenPoints[i]
                    val p2 = screenPoints[i + 1]
                    drawLine(
                        color = primaryColor,
                        start = p1,
                        end = p2,
                        strokeWidth = with(density) { 3.5.dp.toPx() },
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 12f), 0f)
                    )
                }
            }

            // 3. Draw User Location if available
            userLocation?.let { uLoc ->
                val uScreen = toScreenCoords(uLoc.latitude, uLoc.longitude)

                // Translucent accuracy halo
                drawCircle(
                    color = Color(0x332196F3),
                    radius = with(density) { 26.dp.toPx() },
                    center = uScreen
                )
                // Pulse halo ring
                drawCircle(
                    color = Color(0xFF2196F3),
                    radius = with(density) { 14.dp.toPx() },
                    center = uScreen,
                    style = Stroke(width = with(density) { 2.dp.toPx() })
                )
                // Core solid dot
                drawCircle(
                    color = Color(0xFF1565C0),
                    radius = with(density) { 8.dp.toPx() },
                    center = uScreen
                )
                // White center pip
                drawCircle(
                    color = Color.White,
                    radius = with(density) { 3.dp.toPx() },
                    center = uScreen
                )

                // Native paint for text label
                drawContext.canvas.nativeCanvas.apply {
                    val labelPaint = Paint().apply {
                        color = Color(0xFF0D47A1).toArgb()
                        textSize = with(density) { 11.sp.toPx() }
                        typeface = Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val bgPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        style = Paint.Style.FILL
                        isAntiAlias = true
                        setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(60, 0, 0, 0))
                    }

                    val userText = if (isEs) "📍 Tú estás aquí" else "📍 You are here"
                    val textY = uScreen.y - with(density) { 18.dp.toPx() }
                    val textWidth = labelPaint.measureText(userText)
                    val pad = with(density) { 6.dp.toPx() }

                    drawRoundRect(
                        uScreen.x - textWidth / 2f - pad,
                        textY - with(density) { 12.dp.toPx() },
                        uScreen.x + textWidth / 2f + pad,
                        textY + pad / 2f,
                        with(density) { 6.dp.toPx() },
                        with(density) { 6.dp.toPx() },
                        bgPaint
                    )
                    drawText(userText, uScreen.x, textY, labelPaint)
                }
            }

            // 4. Draw Stop Markers (numbered badges)
            validStops.forEachIndexed { index, stop ->
                val center = toScreenCoords(stop.latitude, stop.longitude)
                val isSelected = selectedStop?.id == stop.id
                val radius = if (isSelected) selectedMarkerRadiusPx else markerRadiusPx

                val markerColor = when (stop.category) {
                    StopCategory.SIGHTSEEING -> Color(0xFF6750A4)
                    StopCategory.TRANSPORT -> Color(0xFF0061A4)
                    StopCategory.FOOD -> Color(0xFFC0392B)
                    StopCategory.HOTEL -> Color(0xFF006C4C)
                    StopCategory.ACTIVITY -> Color(0xFFD35400)
                    StopCategory.SHOPPING -> Color(0xFF984061)
                }

                // If selected, draw highlight halo
                if (isSelected) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.35f),
                        radius = radius + with(density) { 8.dp.toPx() },
                        center = center
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = radius + with(density) { 4.dp.toPx() },
                        center = center,
                        style = Stroke(width = with(density) { 2.dp.toPx() })
                    )
                }

                // Drop shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.25f),
                    radius = radius + 2f,
                    center = Offset(center.x, center.y + 4f)
                )

                // Marker background
                drawCircle(
                    color = markerColor,
                    radius = radius,
                    center = center
                )

                // White border
                drawCircle(
                    color = Color.White,
                    radius = radius,
                    center = center,
                    style = Stroke(width = with(density) { 2.5.dp.toPx() })
                )

                // Marker number text
                drawContext.canvas.nativeCanvas.apply {
                    val textPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = with(density) { (if (isSelected) 14.sp else 12.sp).toPx() }
                        typeface = Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val textOffset = (textPaint.descent() + textPaint.ascent()) / 2
                    drawText("${index + 1}", center.x, center.y - textOffset, textPaint)

                    // Draw stop name pill below marker
                    val namePaint = Paint().apply {
                        color = onSurfaceColor.toArgb()
                        textSize = with(density) { 10.sp.toPx() }
                        typeface = Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val nameBgPaint = Paint().apply {
                        color = surfaceColor.toArgb()
                        style = Paint.Style.FILL
                        isAntiAlias = true
                        setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(50, 0, 0, 0))
                    }

                    val shortTitle = if (stop.title.length > 18) stop.title.take(16) + "…" else stop.title
                    val nameText = "${stop.startTime.take(5)} $shortTitle"
                    val nameW = namePaint.measureText(nameText)
                    val labelY = center.y + radius + with(density) { 14.dp.toPx() }
                    val pad = with(density) { 5.dp.toPx() }

                    drawRoundRect(
                        center.x - nameW / 2f - pad,
                        labelY - with(density) { 10.dp.toPx() },
                        center.x + nameW / 2f + pad,
                        labelY + pad / 2f,
                        with(density) { 4.dp.toPx() },
                        with(density) { 4.dp.toPx() },
                        nameBgPaint
                    )
                    drawText(nameText, center.x, labelY, namePaint)
                }
            }
        }

        // Top Summary Bar: Total Stops & Kilometers
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🚶", fontSize = 13.sp)
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        if (isEs) "%d visitas · %.1f km" else "%d stops · %.1f km",
                        validStops.size,
                        totalRouteKm
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Bottom Map Controls: Zoom in/out, Fit Route, Center User, Open External Route
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Button: Open Google Maps Route
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 3.dp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Button(
                    onClick = onOpenExternalRoute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🗺️", fontSize = 13.sp)
                        Text(
                            text = if (isEs) "Ruta en Maps" else "Route in Maps",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Controls card: Reset Fit, User Location, Zoom In, Zoom Out
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Zoom In
                    IconButton(
                        onClick = { zoomScale = (zoomScale * 1.35f).coerceAtMost(10.0f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(
                            text = "+",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Zoom Out
                    IconButton(
                        onClick = { zoomScale = (zoomScale / 1.35f).coerceAtLeast(0.5f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("—", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Fit Route (Reset)
                    IconButton(
                        onClick = {
                            zoomScale = 1.0f
                            panOffset = Offset.Zero
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("🔍", fontSize = 16.sp)
                    }

                    // Center on User Location
                    IconButton(
                        onClick = {
                            if (!hasLocationPermission) {
                                onRequestLocationPermission()
                            } else if (userLocation != null) {
                                // Pan to user location
                                val (nx, ny) = GeoProjectionUtils.projectToMercatorNormalized(
                                    userLocation.latitude,
                                    userLocation.longitude
                                )
                                val (minNormX, minNormY) = GeoProjectionUtils.projectToMercatorNormalized(
                                    boundingBox.maxLat,
                                    boundingBox.minLng
                                )
                                val (maxNormX, maxNormY) = GeoProjectionUtils.projectToMercatorNormalized(
                                    boundingBox.minLat,
                                    boundingBox.maxLng
                                )
                                val spanNormX = max(maxNormX - minNormX, 0.00001)
                                val spanNormY = max(maxNormY - minNormY, 0.00001)

                                val basePxX = ((nx - minNormX) / spanNormX).toFloat() * 700f + 100f
                                val basePxY = ((ny - minNormY) / spanNormY).toFloat() * 700f + 100f

                                panOffset = Offset(-basePxX + 450f, -basePxY + 450f)
                                zoomScale = 2.0f
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("🎯", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
