package com.laralbir.japantraveltools.feature.planner.map

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
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
    val context = LocalContext.current
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
                        text = if (isEs) "Edita o añade visitas con coordenadas geográficas para ver el mapa interactivo."
                        else "Edit or add stops with geographic coordinates to see the interactive map.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    // Load local Leaflet CSS and JS directly from assets into memory to avoid any file:/// CORS blocking
    val leafletCss = remember {
        runCatching {
            context.assets.open("leaflet/leaflet.css").bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }
    val leafletJs = remember {
        runCatching {
            context.assets.open("leaflet/leaflet.js").bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }

    val currentStops by rememberUpdatedState(validStops)
    val currentOnStopClick by rememberUpdatedState(onStopClick)

    var selectedLayer by remember { mutableStateOf(MapTileLayerType.GOOGLE_STREETS) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    fun buildStopsJson(): String {
        val array = JSONArray()
        validStops.forEach { stop ->
            val color = when (stop.category) {
                StopCategory.SIGHTSEEING -> "#6750A4"
                StopCategory.TRANSPORT -> "#0061A4"
                StopCategory.FOOD -> "#C0392B"
                StopCategory.HOTEL -> "#006C4C"
                StopCategory.ACTIVITY -> "#D35400"
                StopCategory.SHOPPING -> "#984061"
            }
            val obj = JSONObject().apply {
                put("id", stop.id)
                put("title", stop.title)
                put("time", stop.startTime)
                put("lat", stop.latitude)
                put("lng", stop.longitude)
                put("color", color)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun updateMapData(wv: WebView?) {
        val view = wv ?: return
        val stopsJson = buildStopsJson()
        val selectedId = selectedStop?.id.orEmpty()
        val uLat = userLocation?.latitude ?: 0.0
        val uLng = userLocation?.longitude ?: 0.0

        val js = """
            if (typeof resizeMapToViewport === 'function') {
                resizeMapToViewport();
            } else if (typeof map !== 'undefined' && map) {
                map.invalidateSize();
            }
            if (typeof renderMarkers === 'function') {
                renderMarkers($stopsJson, '$selectedId');
                renderUserLocation($uLat, $uLng);
            }
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    LaunchedEffect(validStops, selectedStop, userLocation, isMapLoaded) {
        if (isMapLoaded) {
            updateMapData(webViewRef)
        }
    }

    LaunchedEffect(selectedLayer, isEs, isMapLoaded) {
        if (isMapLoaded) {
            val tileUrl = selectedLayer.resolveTileUrl(isEs)
            webViewRef?.evaluateJavascript(
                "if (typeof setTileLayer === 'function') { setTileLayer('$tileUrl'); }",
                null
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        // Embedded Leaflet Map WebView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView.setWebContentsDebuggingEnabled(true)
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "JapanTravelTools/1.2.0 (Android; MobileApp)"
                    }

                    // Prevent parent Compose scroll containers from intercepting map touch gestures
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.e("DayItineraryMap", "WebView Console: [${consoleMessage?.messageLevel()}] ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                            return true
                        }
                    }

                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onStopClicked(stopId: String) {
                                val clicked = currentStops.firstOrNull { it.id == stopId }
                                if (clicked != null) {
                                    Handler(Looper.getMainLooper()).post {
                                        currentOnStopClick(clicked)
                                    }
                                }
                            }
                        },
                        "AndroidBridge"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.e("DayItineraryMap", "onPageFinished: $url")
                            isMapLoaded = true
                            view?.postDelayed({
                                updateMapData(view)
                            }, 100)
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            Log.e("DayItineraryMap", "onReceivedError: ${error?.errorCode} ${error?.description} for ${request?.url}")
                        }
                    }

                    val initialStopsJson = buildStopsJson()
                    val initialUserLat = userLocation?.latitude ?: 0.0
                    val initialUserLng = userLocation?.longitude ?: 0.0
                    val initialSelectedId = selectedStop?.id.orEmpty()
                    val initialTileUrl = selectedLayer.resolveTileUrl(isEs)

                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=10.0, user-scalable=yes">
                            <title>Japan Travel Tools Map</title>
                            <style>
                                $leafletCss
                                html, body {
                                    margin: 0;
                                    padding: 0;
                                    width: 100vw;
                                    height: 100vh;
                                    overflow: hidden;
                                    background-color: #f6f5f0;
                                }
                                #map {
                                    position: fixed;
                                    top: 0;
                                    left: 0;
                                    right: 0;
                                    bottom: 0;
                                    width: 100vw;
                                    height: 100vh;
                                    background-color: #f6f5f0;
                                    background-image: 
                                        linear-gradient(rgba(0, 0, 0, 0.06) 1px, transparent 1px),
                                        linear-gradient(90deg, rgba(0, 0, 0, 0.06) 1px, transparent 1px);
                                    background-size: 36px 36px;
                                }
                                .marker-badge {
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    border-radius: 50%;
                                    color: #ffffff;
                                    font-weight: 800;
                                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                                    border: 2.5px solid #ffffff;
                                    box-shadow: 0 3px 8px rgba(0, 0, 0, 0.45);
                                    cursor: pointer;
                                    transition: transform 0.15s ease;
                                }
                                .marker-badge:hover, .marker-badge:active {
                                    transform: scale(1.18);
                                }
                                .marker-selected {
                                    outline: 3.5px solid #1976D2;
                                    outline-offset: 2px;
                                    box-shadow: 0 0 12px rgba(25, 118, 210, 0.85);
                                }
                                .user-dot {
                                    width: 16px;
                                    height: 16px;
                                    background: #1976D2;
                                    border: 3px solid #ffffff;
                                    border-radius: 50%;
                                    box-shadow: 0 0 0 6px rgba(33, 150, 243, 0.35);
                                }
                                .stop-tooltip {
                                    background-color: rgba(255, 255, 255, 0.94);
                                    border: 1px solid #cccccc;
                                    border-radius: 6px;
                                    padding: 3px 7px;
                                    font-size: 11px;
                                    font-weight: 700;
                                    color: #111111;
                                    box-shadow: 0 2px 6px rgba(0,0,0,0.22);
                                    white-space: nowrap;
                                }
                            </style>
                        </head>
                        <body>
                        <div id="map"></div>
                        <script>
                            window.onerror = function(msg, url, line) {
                                console.error('JS Error: ' + msg + ' at ' + url + ':' + line);
                            };

                            $leafletJs

                            var map = L.map('map', {
                                zoomControl: false,
                                attributionControl: false
                            }).setView([35.6762, 139.6503], 12);

                            var currentTileLayer = L.tileLayer('$initialTileUrl', {
                                maxZoom: 19
                            }).addTo(map);

                            function setTileLayer(url) {
                                if (currentTileLayer && map) {
                                    map.removeLayer(currentTileLayer);
                                }
                                currentTileLayer = L.tileLayer(url, {
                                    maxZoom: 19
                                }).addTo(map);
                            }

                            var markersLayer = L.layerGroup().addTo(map);
                            var userLayer = L.layerGroup().addTo(map);
                            var lastBounds = null;

                            if (window.ResizeObserver) {
                                var resizeObserver = new ResizeObserver(function() {
                                    if (map) { map.invalidateSize(); }
                                });
                                resizeObserver.observe(document.getElementById('map'));
                            }

                            function renderMarkers(stopsJson, selectedId) {
                                if (map) {
                                    map.invalidateSize();
                                }
                                markersLayer.clearLayers();
                                var bounds = [];
                                if (!stopsJson || stopsJson.length === 0) return;

                                stopsJson.forEach(function(stop, index) {
                                    var num = index + 1;
                                    var isSelected = stop.id === selectedId;
                                    var size = isSelected ? 36 : 30;
                                    var fontSize = isSelected ? 15 : 13;
                                    var selectedClass = isSelected ? ' marker-selected' : '';

                                    var icon = L.divIcon({
                                        className: 'custom-div-icon',
                                        html: '<div class="marker-badge' + selectedClass + '" style="width:' + size + 'px; height:' + size + 'px; background-color:' + stop.color + '; font-size:' + fontSize + 'px;">' + num + '</div>',
                                        iconSize: [size, size],
                                        iconAnchor: [size / 2, size / 2]
                                    });

                                    var marker = L.marker([stop.lat, stop.lng], { icon: icon }).addTo(markersLayer);
                                    
                                    var tooltipText = (stop.time ? stop.time.substring(0, 5) + ' ' : '') + stop.title;
                                    marker.bindTooltip(tooltipText, {
                                        permanent: true,
                                        direction: 'bottom',
                                        offset: [0, size / 2 + 2],
                                        className: 'stop-tooltip'
                                    });

                                    marker.on('click', function() {
                                        if (window.AndroidBridge && window.AndroidBridge.onStopClicked) {
                                            window.AndroidBridge.onStopClicked(stop.id);
                                        }
                                    });

                                    bounds.push([stop.lat, stop.lng]);
                                });

                                if (bounds.length > 0) {
                                    lastBounds = L.latLngBounds(bounds);
                                    map.fitBounds(lastBounds, { padding: [50, 50], maxZoom: 16 });
                                }
                            }

                            function renderUserLocation(userLat, userLng) {
                                userLayer.clearLayers();
                                if (!userLat || !userLng || (userLat === 0 && userLng === 0)) return;
                                var icon = L.divIcon({
                                    className: 'user-icon',
                                    html: '<div class="user-dot"></div>',
                                    iconSize: [16, 16],
                                    iconAnchor: [8, 8]
                                });
                                var marker = L.marker([userLat, userLng], { icon: icon }).addTo(userLayer);
                                marker.bindTooltip('📍 Tú estás aquí', { permanent: false, direction: 'top' });
                            }

                            function resizeMapToViewport() {
                                var h = window.innerHeight || document.documentElement.clientHeight || 500;
                                var w = window.innerWidth || document.documentElement.clientWidth || 400;
                                var el = document.getElementById('map');
                                if (el) {
                                    el.style.width = w + 'px';
                                    el.style.height = h + 'px';
                                }
                                if (map) {
                                    map.invalidateSize();
                                }
                            }

                            function fitToMarkers() {
                                resizeMapToViewport();
                                if (lastBounds && lastBounds.isValid && lastBounds.isValid()) {
                                    map.fitBounds(lastBounds, { padding: [50, 50], maxZoom: 16, animate: true });
                                } else if (lastBounds) {
                                    map.fitBounds(lastBounds, { padding: [50, 50], maxZoom: 16, animate: true });
                                }
                            }

                            function panToUser(lat, lng) {
                                if (lat !== 0 || lng !== 0) {
                                    map.setView([lat, lng], 15);
                                }
                            }

                            function zoomIn() { map.zoomIn(); }
                            function zoomOut() { map.zoomOut(); }

                            function invalidateAndFit() {
                                resizeMapToViewport();
                                if (lastBounds && lastBounds.isValid && lastBounds.isValid()) {
                                    map.fitBounds(lastBounds, { padding: [50, 50], maxZoom: 16 });
                                }
                            }

                            window.addEventListener('resize', invalidateAndFit);
                            window.addEventListener('orientationchange', invalidateAndFit);
                            resizeMapToViewport();
                            setTimeout(invalidateAndFit, 80);
                            setTimeout(invalidateAndFit, 250);
                            setTimeout(invalidateAndFit, 600);

                            // Render initial stops right away
                            renderMarkers($initialStopsJson, '$initialSelectedId');
                            renderUserLocation($initialUserLat, $initialUserLng);
                        </script>
                        </body>
                        </html>
                    """.trimIndent()

                    loadDataWithBaseURL("https://tile.openstreetmap.org/", html, "text/html", "utf-8", "https://tile.openstreetmap.org/")
                    webViewRef = this
                }
            },
            update = { wv ->
                if (isMapLoaded) {
                    updateMapData(wv)
                }
            }
        )

        // Top Summary Bar: Total Stops Count (No connecting line)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("📍", fontSize = 13.sp)
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        if (isEs) "%d visitas" else "%d stops",
                        validStops.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Bottom-Start: Configurable Map Layer Selector (Google Maps | Satélite | OSM)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapTileLayerType.entries.forEach { layer ->
                    val isSelected = selectedLayer == layer
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedLayer = layer }
                    ) {
                        Text(
                            text = layer.shortLabel(isEs),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
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
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Zoom In
                    IconButton(
                        onClick = { webViewRef?.evaluateJavascript("zoomIn()", null) },
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
                        onClick = { webViewRef?.evaluateJavascript("zoomOut()", null) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("—", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Fit to markers (Centrar visitas)
                    IconButton(
                        onClick = { webViewRef?.evaluateJavascript("fitToMarkers()", null) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("📍", fontSize = 16.sp)
                    }

                    // Center on User Location
                    IconButton(
                        onClick = {
                            if (!hasLocationPermission) {
                                onRequestLocationPermission()
                            } else if (userLocation != null) {
                                webViewRef?.evaluateJavascript(
                                    "panToUser(${userLocation.latitude}, ${userLocation.longitude})",
                                    null
                                )
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        MyLocationGpsIcon()
                    }
                }
            }
        }
    }
}

@Composable
private fun MyLocationGpsIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF1976D2)
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = 1.8.dp.toPx()
        val ringRadius = 6.2.dp.toPx()
        val dotRadius = 3.dp.toPx()
        val tickOuter = 9.2.dp.toPx()

        // Outer ring
        drawCircle(
            color = tint,
            radius = ringRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // Solid center dot
        drawCircle(
            color = tint,
            radius = dotRadius,
            center = center
        )

        // North tick
        drawLine(
            color = tint,
            start = Offset(center.x, center.y - tickOuter),
            end = Offset(center.x, center.y - ringRadius),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // South tick
        drawLine(
            color = tint,
            start = Offset(center.x, center.y + ringRadius),
            end = Offset(center.x, center.y + tickOuter),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // West tick
        drawLine(
            color = tint,
            start = Offset(center.x - tickOuter, center.y),
            end = Offset(center.x - ringRadius, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // East tick
        drawLine(
            color = tint,
            start = Offset(center.x + ringRadius, center.y),
            end = Offset(center.x + tickOuter, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
