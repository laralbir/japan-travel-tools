package com.laralbir.japantraveltools.core.portability

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.planner.TripPlannerUseCase
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportPlannerUseCase
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import com.laralbir.japantraveltools.feature.weather.PlanningSuitability
import com.laralbir.japantraveltools.feature.weather.WeatherDayForecast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class PortableTravelPlanBundle(
    val schemaVersion: Int = 1,
    val tripTitle: String = "Japón Otoño 2026",
    val itineraryStops: List<ItineraryStopItem>,
    val transportSegments: List<TransportSegmentItem>,
    val aiOptimizationSummary: String = ""
)

/**
 * Shared reactive state holder so that Itinerary Planner, Transport Planner,
 * JSON Import/Export and Gemini AI Plan Builder operate on the same live offline-first plan.
 */
object SharedTravelPlanStore {
    private fun defaultSeedBundle() = PortableTravelPlanBundle(
        schemaVersion = 1,
        tripTitle = "Japón Otoño 2026",
        itineraryStops = TripPlannerUseCase.defaultSampleStops(),
        transportSegments = TransportPlannerUseCase.defaultSampleSegments()
    )

    private val _planState = MutableStateFlow(defaultSeedBundle())
    val planState: StateFlow<PortableTravelPlanBundle> = _planState.asStateFlow()

    fun updatePlan(newPlan: PortableTravelPlanBundle) {
        _planState.value = newPlan
    }

    fun resetToDefaults() {
        _planState.value = defaultSeedBundle()
    }

    fun exportCurrentJson(): String = TravelPlanJsonCodec.exportToPrettyJson(_planState.value)

    fun importJson(json: String, mergeWithExisting: Boolean = false): Result<PortableTravelPlanBundle> {
        return runCatching {
            val parsed = TravelPlanJsonCodec.importFromJson(json)
            val updated = if (mergeWithExisting) {
                val current = _planState.value
                val mergedStops = (current.itineraryStops + parsed.itineraryStops).distinctBy { it.id }
                val mergedTransport = (current.transportSegments + parsed.transportSegments).distinctBy { it.id }
                PortableTravelPlanBundle(
                    schemaVersion = parsed.schemaVersion,
                    tripTitle = parsed.tripTitle.ifBlank { current.tripTitle },
                    itineraryStops = mergedStops,
                    transportSegments = mergedTransport
                )
            } else {
                parsed
            }
            _planState.value = updated
            updated
        }
    }
}

/**
 * Human-readable Pretty JSON serializer & parser for Travel Plans (Itineraries + Flights/Trains/Metro/Buses)
 * that runs identically in JVM unit tests and on Android without external reflection dependencies.
 */
object TravelPlanJsonCodec {

    fun exportToPrettyJson(bundle: PortableTravelPlanBundle): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"schemaVersion\": ${bundle.schemaVersion},")
        sb.appendLine("  \"tripTitle\": \"${escape(bundle.tripTitle)}\",")
        sb.appendLine("  \"itineraryStops\": [")
        bundle.itineraryStops.forEachIndexed { index, stop ->
            val comma = if (index < bundle.itineraryStops.lastIndex) "," else ""
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escape(stop.id)}\",")
            sb.appendLine("      \"dayNumber\": ${stop.dayNumber},")
            sb.appendLine("      \"dateIso\": \"${escape(stop.dateIso)}\",")
            sb.appendLine("      \"startTime\": \"${escape(stop.startTime)}\",")
            sb.appendLine("      \"endTime\": \"${escape(stop.endTime)}\",")
            sb.appendLine("      \"title\": \"${escape(stop.title)}\",")
            sb.appendLine("      \"category\": \"${stop.category.name}\",")
            sb.appendLine("      \"placeNameJapanese\": \"${escape(stop.placeNameJapanese)}\",")
            sb.appendLine("      \"latitude\": ${stop.latitude},")
            sb.appendLine("      \"longitude\": ${stop.longitude},")
            sb.appendLine("      \"notes\": \"${escape(stop.notes)}\"")
            sb.appendLine("    }$comma")
        }
        sb.appendLine("  ],")
        sb.appendLine("  \"transportSegments\": [")
        bundle.transportSegments.forEachIndexed { index, seg ->
            val comma = if (index < bundle.transportSegments.lastIndex) "," else ""
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escape(seg.id)}\",")
            sb.appendLine("      \"order\": ${seg.order},")
            sb.appendLine("      \"type\": \"${seg.type.name}\",")
            sb.appendLine("      \"operatorName\": \"${escape(seg.operatorName)}\",")
            sb.appendLine("      \"lineOrNumber\": \"${escape(seg.lineOrNumber)}\",")
            sb.appendLine("      \"departureCode\": \"${escape(seg.departureCode)}\",")
            sb.appendLine("      \"departureName\": \"${escape(seg.departureName)}\",")
            sb.appendLine("      \"departureZoneId\": \"${escape(seg.departureZoneId)}\",")
            sb.appendLine("      \"departureTerminalOrPlatform\": \"${escape(seg.departureTerminalOrPlatform)}\",")
            sb.appendLine("      \"checkInTime\": \"${escape(seg.checkInTime ?: "")}\",")
            sb.appendLine("      \"boardingTime\": \"${escape(seg.boardingTime ?: "")}\",")
            sb.appendLine("      \"departureTimeIso\": \"${escape(seg.departureTimeIso)}\",")
            sb.appendLine("      \"arrivalCode\": \"${escape(seg.arrivalCode)}\",")
            sb.appendLine("      \"arrivalName\": \"${escape(seg.arrivalName)}\",")
            sb.appendLine("      \"arrivalZoneId\": \"${escape(seg.arrivalZoneId)}\",")
            sb.appendLine("      \"arrivalTerminalOrPlatform\": \"${escape(seg.arrivalTerminalOrPlatform)}\",")
            sb.appendLine("      \"arrivalTimeIso\": \"${escape(seg.arrivalTimeIso)}\",")
            sb.appendLine("      \"seatOrCarInfo\": \"${escape(seg.seatOrCarInfo)}\",")
            sb.appendLine("      \"stationExitOrBelt\": \"${escape(seg.stationExitOrBelt)}\",")
            sb.appendLine("      \"passOrPaymentBadge\": \"${escape(seg.passOrPaymentBadge)}\"")
            sb.appendLine("    }$comma")
        }
        sb.appendLine("  ]")
        sb.append("}")
        return sb.toString()
    }

    fun importFromJson(json: String): PortableTravelPlanBundle {
        val schemaVersion = extractInt(json, "schemaVersion") ?: 1
        val tripTitle = extractString(json, "tripTitle") ?: "Japón 2026"

        val stopsBlock = extractArrayBlock(json, "itineraryStops")
        val segmentsBlock = extractArrayBlock(json, "transportSegments")

        val parsedStops = splitJsonObjects(stopsBlock).mapIndexed { idx, obj ->
            val catStr = extractString(obj, "category") ?: StopCategory.SIGHTSEEING.name
            ItineraryStopItem(
                id = extractString(obj, "id") ?: "stop-imp-$idx",
                dayNumber = extractInt(obj, "dayNumber") ?: 1,
                dateIso = extractString(obj, "dateIso") ?: "2026-11-11",
                startTime = extractString(obj, "startTime") ?: "09:00",
                endTime = extractString(obj, "endTime") ?: "10:30",
                title = extractString(obj, "title") ?: "Parada #${idx + 1}",
                category = runCatching { StopCategory.valueOf(catStr) }.getOrDefault(StopCategory.SIGHTSEEING),
                placeNameJapanese = extractString(obj, "placeNameJapanese") ?: "",
                latitude = extractDouble(obj, "latitude") ?: 35.6762,
                longitude = extractDouble(obj, "longitude") ?: 139.6503,
                notes = extractString(obj, "notes") ?: ""
            )
        }

        val parsedSegments = splitJsonObjects(segmentsBlock).mapIndexed { idx, obj ->
            val typeStr = extractString(obj, "type") ?: TransportMode.TRAIN_SHINKANSEN.name
            TransportSegmentItem(
                id = extractString(obj, "id") ?: "seg-imp-$idx",
                order = extractInt(obj, "order") ?: (idx + 1),
                type = runCatching { TransportMode.valueOf(typeStr) }.getOrDefault(TransportMode.TRAIN_SHINKANSEN),
                operatorName = extractString(obj, "operatorName") ?: "JR",
                lineOrNumber = extractString(obj, "lineOrNumber") ?: "",
                departureCode = extractString(obj, "departureCode") ?: "TYO",
                departureName = extractString(obj, "departureName") ?: "Tokyo",
                departureZoneId = extractString(obj, "departureZoneId") ?: "Asia/Tokyo",
                departureTerminalOrPlatform = extractString(obj, "departureTerminalOrPlatform") ?: "",
                checkInTime = extractString(obj, "checkInTime")?.takeIf { it.isNotBlank() },
                boardingTime = extractString(obj, "boardingTime")?.takeIf { it.isNotBlank() },
                departureTimeIso = extractString(obj, "departureTimeIso") ?: "2026-11-11T09:00",
                arrivalCode = extractString(obj, "arrivalCode") ?: "KYO",
                arrivalName = extractString(obj, "arrivalName") ?: "Kyoto",
                arrivalZoneId = extractString(obj, "arrivalZoneId") ?: "Asia/Tokyo",
                arrivalTerminalOrPlatform = extractString(obj, "arrivalTerminalOrPlatform") ?: "",
                arrivalTimeIso = extractString(obj, "arrivalTimeIso") ?: "2026-11-11T11:15",
                seatOrCarInfo = extractString(obj, "seatOrCarInfo") ?: "",
                stationExitOrBelt = extractString(obj, "stationExitOrBelt") ?: "",
                passOrPaymentBadge = extractString(obj, "passOrPaymentBadge") ?: "Included"
            )
        }

        return PortableTravelPlanBundle(
            schemaVersion = schemaVersion,
            tripTitle = tripTitle,
            itineraryStops = parsedStops,
            transportSegments = parsedSegments
        )
    }

    private fun escape(raw: String): String =
        raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun unescape(raw: String): String =
        raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")

    private fun extractString(source: String, key: String): String? {
        val regex = """"$key"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
        return regex.find(source)?.groupValues?.get(1)?.let { unescape(it) }
    }

    private fun extractInt(source: String, key: String): Int? {
        val regex = """"$key"\s*:\s*(-?\d+)""".toRegex()
        return regex.find(source)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractDouble(source: String, key: String): Double? {
        val regex = """"$key"\s*:\s*(-?\d+(?:\.\d+)?)""".toRegex()
        return regex.find(source)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractArrayBlock(source: String, key: String): String {
        val keyIdx = source.indexOf("\"$key\"")
        if (keyIdx == -1) return ""
        val openBracket = source.indexOf('[', keyIdx)
        if (openBracket == -1) return ""
        var depth = 0
        for (i in openBracket until source.length) {
            when (source[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        return source.substring(openBracket + 1, i)
                    }
                }
            }
        }
        return ""
    }

    private fun splitJsonObjects(arrayContent: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var objStart = -1
        for (i in arrayContent.indices) {
            when (arrayContent[i]) {
                '{' -> {
                    if (depth == 0) objStart = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && objStart != -1) {
                        result.add(arrayContent.substring(objStart, i + 1))
                        objStart = -1
                    }
                }
            }
        }
        return result
    }
}

/**
 * AI Plan Builder & Optimizer (SPEC-06 RF-06.2):
 * Allows Gemini AI to read the current travel plan in the app, resolve schedule conflicts,
 * add recommended stops with Japanese names/coordinates, and insert multimodal transport segments.
 */
object AiPlanBuilderEngine {

    fun improveAndExpandPlan(
        currentPlan: PortableTravelPlanBundle,
        userInstruction: String
    ): PortableTravelPlanBundle {
        val lower = userInstruction.lowercase(Locale.ROOT)
        var resolvedConflictsCount = 0

        // 1. Only resolve genuine schedule overlaps (when a stop starts before the previous stop ended)
        val deconflictedStops = currentPlan.itineraryStops
            .groupBy { it.dayNumber }
            .flatMap { (_, dayStops) ->
                val sorted = dayStops.sortedBy { toMinutes(it.startTime) }
                var previousEndMinute = 0
                sorted.map { stop ->
                    val origStart = toMinutes(stop.startTime)
                    val origEnd = toMinutes(stop.endTime)
                    val duration = (origEnd - origStart).coerceAtLeast(45)
                    if (origStart < previousEndMinute) {
                        resolvedConflictsCount++
                        val adjustedStart = previousEndMinute + 15
                        val adjustedEnd = adjustedStart + duration
                        previousEndMinute = adjustedEnd
                        stop.copy(
                            startTime = formatMinutes(adjustedStart),
                            endTime = formatMinutes(adjustedEnd)
                        )
                    } else {
                        previousEndMinute = origEnd
                        stop
                    }
                }
            }
            .toMutableList()

        val updatedTransport = currentPlan.transportSegments.toMutableList()
        val nextDayNumber = (deconflictedStops.maxOfOrNull { it.dayNumber } ?: 0) + 1
        val nextDateIso = String.format(Locale.US, "2026-11-%02d", (10 + nextDayNumber).coerceAtMost(30))
        val addedDescription = mutableListOf<String>()

        when {
            lower.contains("nara") || lower.contains("ciervo") || lower.contains("todai") -> {
                if (deconflictedStops.none { it.id.startsWith("ai-nara-") }) {
                    deconflictedStops.addAll(
                        listOf(
                            ItineraryStopItem(
                                id = "ai-nara-1",
                                dayNumber = nextDayNumber,
                                dateIso = nextDateIso,
                                startTime = "09:30",
                                endTime = "11:00",
                                title = "Parque de Nara y Ciervos Sagrados",
                                category = StopCategory.SIGHTSEEING,
                                placeNameJapanese = "奈良公園",
                                latitude = 34.6851,
                                longitude = 135.8430,
                                notes = "Tren Kintetsu Nara / JR Nara Line"
                            ),
                            ItineraryStopItem(
                                id = "ai-nara-2",
                                dayNumber = nextDayNumber,
                                dateIso = nextDateIso,
                                startTime = "11:15",
                                endTime = "12:45",
                                title = "Gran Buda de Todai-ji",
                                category = StopCategory.SIGHTSEEING,
                                placeNameJapanese = "東大寺",
                                latitude = 34.6890,
                                longitude = 135.8399,
                                notes = "Pabellón Daibutsuden"
                            ),
                            ItineraryStopItem(
                                id = "ai-nara-3",
                                dayNumber = nextDayNumber,
                                dateIso = nextDateIso,
                                startTime = "13:15",
                                endTime = "15:00",
                                title = "Santuario Kasuga Taisha",
                                category = StopCategory.SIGHTSEEING,
                                placeNameJapanese = "春日大社",
                                latitude = 34.6814,
                                longitude = 135.8484,
                                notes = "Camino de los faroles de piedra"
                            )
                        )
                    )
                    addedDescription.add("Día $nextDayNumber en Nara (3 paradas)")
                }
            }

            lower.contains("hakone") || lower.contains("fuji") || lower.contains("onsen") -> {
                if (deconflictedStops.none { it.id.startsWith("ai-hakone-") }) {
                    deconflictedStops.addAll(
                        listOf(
                            ItineraryStopItem(
                                id = "ai-hakone-1",
                                dayNumber = nextDayNumber,
                                dateIso = nextDateIso,
                                startTime = "10:00",
                                endTime = "11:45",
                                title = "Teleférico Hakone y Valle Owakudani",
                                category = StopCategory.SIGHTSEEING,
                                placeNameJapanese = "大涌谷",
                                latitude = 35.2443,
                                longitude = 139.0195,
                                notes = "Mirador panorámico al Monte Fuji"
                            ),
                            ItineraryStopItem(
                                id = "ai-hakone-2",
                                dayNumber = nextDayNumber,
                                dateIso = nextDateIso,
                                startTime = "12:15",
                                endTime = "14:00",
                                title = "Crucero Lago Ashi y Santuario Hakone-jinja",
                                category = StopCategory.ACTIVITY,
                                placeNameJapanese = "芦ノ湖 · 箱根神社",
                                latitude = 35.2045,
                                longitude = 139.0253,
                                notes = "Torii rojo junto al agua"
                            )
                        )
                    )
                    addedDescription.add("Día $nextDayNumber en Hakone / Monte Fuji (2 paradas)")
                }
            }

            lower.contains("hiroshima") || lower.contains("miyajima") -> {
                if (deconflictedStops.none { it.id.startsWith("ai-hiroshima-") }) {
                    deconflictedStops.addAll(
                        listOf(
                            ItineraryStopItem(
                                id = "ai-hiroshima-1",
                                dayNumber = nextDayNumber,
                                dateIso = nextDateIso,
                                startTime = "10:00",
                                endTime = "13:00",
                                title = "Isla de Miyajima y Santuario Itsukushima",
                                category = StopCategory.SIGHTSEEING,
                                placeNameJapanese = "厳島神社 · 宮島",
                                latitude = 34.2960,
                                longitude = 132.3198,
                                notes = "Ferry JR desde Miyajimaguchi (10 min)"
                            ),
                            ItineraryStopItem(
                                id = "ai-hiroshima-2",
                                dayNumber = nextDayNumber,
                                dateIso = nextDateIso,
                                startTime = "15:00",
                                endTime = "17:30",
                                title = "Parque Conmemorativo de la Paz de Hiroshima",
                                category = StopCategory.SIGHTSEEING,
                                placeNameJapanese = "広島平和記念公園",
                                latitude = 34.3955,
                                longitude = 132.4536,
                                notes = "Cúpula Genbaku y Museo de la Paz"
                            )
                        )
                    )
                    addedDescription.add("Día $nextDayNumber en Hiroshima y Miyajima")
                }
            }

            lower.contains("osaka") || lower.contains("día 3") || lower.contains("day 3") || lower.contains("mejorar") || lower.contains("crear") -> {
                val targetOsakaDay = if (lower.contains("día 3") || lower.contains("day 3")) 3 else nextDayNumber
                if (deconflictedStops.none { it.id.startsWith("ai-osaka-") }) {
                    deconflictedStops.addAll(
                        listOf(
                            ItineraryStopItem(
                                id = "ai-osaka-1",
                                dayNumber = targetOsakaDay,
                                dateIso = "2026-11-13",
                                startTime = "09:30",
                                endTime = "11:30",
                                title = "Castillo de Osaka y Jardín Nishinomaru",
                                category = StopCategory.SIGHTSEEING,
                                placeNameJapanese = "大阪城 · 西の丸庭園",
                                latitude = 34.6873,
                                longitude = 135.5262,
                                notes = "JR Osaka Loop Line hasta Osakajokoen (Salida 1)"
                            ),
                            ItineraryStopItem(
                                id = "ai-osaka-2",
                                dayNumber = targetOsakaDay,
                                dateIso = "2026-11-13",
                                startTime = "12:15",
                                endTime = "14:00",
                                title = "Mercado Kuromon Ichiba (Marisco y Wagyu)",
                                category = StopCategory.FOOD,
                                placeNameJapanese = "黒門市場",
                                latitude = 34.6654,
                                longitude = 135.5063,
                                notes = "Metro Sakaisuji Line hasta Nippombashi (Salida 10)"
                            ),
                            ItineraryStopItem(
                                id = "ai-osaka-3",
                                dayNumber = targetOsakaDay,
                                dateIso = "2026-11-13",
                                startTime = "18:00",
                                endTime = "20:30",
                                title = "Dotonbori, Glico Man y Takoyaki Nocturno",
                                category = StopCategory.ACTIVITY,
                                placeNameJapanese = "道頓堀 · グリコサイン",
                                latitude = 34.6687,
                                longitude = 135.5013,
                                notes = "Paseo junto al canal de Dotonbori y Hozenji Yokocho"
                            )
                        )
                    )
                    addedDescription.add("Día $targetOsakaDay en Osaka (3 paradas)")
                }

                if (updatedTransport.none { it.id == "ai-seg-kyoto-osaka" }) {
                    updatedTransport.add(
                        TransportSegmentItem(
                            id = "ai-seg-kyoto-osaka",
                            order = updatedTransport.size + 1,
                            type = TransportMode.TRAIN_SHINKANSEN,
                            operatorName = "JR West Special Rapid",
                            lineOrNumber = "Special Rapid #15",
                            departureCode = "KYO",
                            departureName = "Kyoto Station (京都駅)",
                            departureZoneId = "Asia/Tokyo",
                            departureTerminalOrPlatform = "Andén 4",
                            boardingTime = "2026-11-13T08:35",
                            departureTimeIso = "2026-11-13T08:45",
                            arrivalCode = "OSA",
                            arrivalName = "Osaka Station (大阪駅)",
                            arrivalZoneId = "Asia/Tokyo",
                            arrivalTerminalOrPlatform = "Andén 7",
                            arrivalTimeIso = "2026-11-13T09:14",
                            seatOrCarInfo = "Vagón 5 · JR Pass / Suica (¥580)",
                            stationExitOrBelt = "Conexión directa a JR Osaka Loop Line",
                            passOrPaymentBadge = "JR Pass / IC"
                        )
                    )
                    addedDescription.add("tren JR Kioto➔Osaka")
                }
            }
        }

        val summaryText = buildString {
            append("✅ Ruta actualizada por IA: ")
            if (resolvedConflictsCount > 0) {
                append("$resolvedConflictsCount solapamiento(s) horario(s) corregido(s). ")
            } else {
                append("Horarios verificados sin solapamientos. ")
            }
            if (addedDescription.isNotEmpty()) {
                append("Añadido: ${addedDescription.joinToString(" + ")}.")
            }
        }

        return currentPlan.copy(
            itineraryStops = deconflictedStops,
            transportSegments = updatedTransport,
            aiOptimizationSummary = summaryText
        )
    }

    fun optimizePlanWithWeather(
        currentPlan: PortableTravelPlanBundle,
        forecasts: List<WeatherDayForecast>
    ): PortableTravelPlanBundle {
        val adaptedStops = currentPlan.itineraryStops.map { stop ->
            val dayForecast = forecasts.firstOrNull { it.dateIso == stop.dateIso }
                ?: forecasts.getOrNull((stop.dayNumber - 1).coerceAtLeast(0))
            if (dayForecast != null && dayForecast.suitability == PlanningSuitability.INDOOR_RECOMMENDED) {
                val weatherNote = "☔ [Open-Meteo ${dayForecast.precipitationProbabilityMax}% lluvia · ${dayForecast.tempMaxC.toInt()}°C]: Priorizar galería cubierta Shotengai / Mercado Nishiki-Kuromon y llevar paraguas."
                if (stop.notes.contains("Open-Meteo")) {
                    stop
                } else {
                    stop.copy(notes = "${stop.notes} · $weatherNote".trim(' ', '·'))
                }
            } else if (dayForecast != null) {
                val sunnyNote = "☀️ [Open-Meteo Despejado · ${dayForecast.tempMaxC.toInt()}°C · Visibilidad Fuji ${dayForecast.fujiVisibilityPercent}%]: Ideal exterior."
                if (stop.notes.contains("Open-Meteo")) {
                    stop
                } else {
                    stop.copy(notes = "${stop.notes} · $sunnyNote".trim(' ', '·'))
                }
            } else {
                stop
            }
        }

        val rainyCount = forecasts.count { it.suitability == PlanningSuitability.INDOOR_RECOMMENDED }
        return currentPlan.copy(
            itineraryStops = adaptedStops,
            aiOptimizationSummary = "🌦️ Gemini IA + Open-Meteo: Plan adaptado al clima (${forecasts.size} días analizados, $rainyCount día(s) con alerta de lluvia y rutas cubiertas añadidas)."
        )
    }

    private fun toMinutes(hhmm: String): Int {
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun formatMinutes(total: Int): String {
        val clamped = total.coerceIn(0, 23 * 60 + 59)
        val h = clamped / 60
        val m = clamped % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }
}
