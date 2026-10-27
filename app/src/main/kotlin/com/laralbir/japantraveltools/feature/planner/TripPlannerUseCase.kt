package com.laralbir.japantraveltools.feature.planner

enum class StopCategory(val labelEn: String, val labelEs: String, val badge: String) {
    SIGHTSEEING("Shrine / Sight", "Templo / Visita", "TORII"),
    FOOD("Ramen / Dining", "Restaurante / Comida", "FOOD"),
    TRANSPORT("Train / Transfer", "Tren / Traslado", "TRAIN"),
    HOTEL("Hotel / Ryokan", "Hotel / Check-in", "HOTEL"),
    SHOPPING("Shopping / Tax-Free", "Compras / Tax-Free", "SHOP"),
    ACTIVITY("Experience / Ticket", "Actividad / Reserva", "TICKET")
}

data class ItineraryStopItem(
    val id: String,
    val dayNumber: Int,
    val dateIso: String,
    val startTime: String,
    val endTime: String,
    val title: String,
    val category: StopCategory,
    val placeNameJapanese: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
    val photoLabels: List<String> = emptyList(),
    val linkedDocumentTitle: String? = null,
    val timeZoneId: String = "Asia/Tokyo",
    val startTimeUtcIso: String? = null,
    val endTimeUtcIso: String? = null
)

data class AnalyzedItineraryStop(
    val item: ItineraryStopItem,
    val orderNumber: Int,
    val hasTimeConflict: Boolean
)

object TripPlannerUseCase {

    fun defaultSampleStops(): List<ItineraryStopItem> = listOf(
        ItineraryStopItem(
            id = "stop-1",
            dayNumber = 1,
            dateIso = "2026-11-11",
            startTime = "08:30",
            endTime = "10:30",
            title = "Senso-ji Temple & Kaminarimon (Asakusa)",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "浅草寺 · 雷門",
            latitude = 35.7147,
            longitude = 139.7966,
            notes = "Metro Ginza Line (G19) Salida 1 · Comprar Omikuji y Ningyo-yaki",
            photoLabels = listOf("Kaminarimon Lantern", "Asakusa Map Exit 1"),
            linkedDocumentTitle = "Guía de Supervivencia en Estaciones de Tokio"
        ),
        ItineraryStopItem(
            id = "stop-2",
            dayNumber = 1,
            dateIso = "2026-11-11",
            startTime = "11:00",
            endTime = "12:30",
            title = "Tokyo Skytree Tembo Deck (350m)",
            category = StopCategory.ACTIVITY,
            placeNameJapanese = "東京スカイツリー",
            latitude = 35.7100,
            longitude = 139.8107,
            notes = "Mostrar código QR de entrada anticipada en planta 4F",
            photoLabels = listOf("QR Entrada Skytree", "Vista Monte Fuji"),
            linkedDocumentTitle = "Entradas y QR"
        ),
        ItineraryStopItem(
            id = "stop-3",
            dayNumber = 1,
            dateIso = "2026-11-11",
            startTime = "13:00",
            endTime = "14:15",
            title = "Ichiran Ramen / Tonkotsu Shinjuku",
            category = StopCategory.FOOD,
            placeNameJapanese = "一蘭 新宿中央東口店",
            latitude = 35.6912,
            longitude = 139.7016,
            notes = "Máquina de tickets en la entrada (efectivo ¥1,180)",
            photoLabels = listOf("Hoja de pedido Ramen")
        ),
        ItineraryStopItem(
            id = "stop-4",
            dayNumber = 1,
            dateIso = "2026-11-11",
            startTime = "15:00",
            endTime = "16:00",
            title = "Check-in Hotel Shinjuku Granbell",
            category = StopCategory.HOTEL,
            placeNameJapanese = "新宿グランベルホテル (歌舞伎町 2-14-5)",
            latitude = 35.6968,
            longitude = 139.7045,
            notes = "Preparar Pasaporte en Bóveda ID para registro obligatorio",
            photoLabels = listOf("Mapa Fachada Hotel"),
            linkedDocumentTitle = "Reserva Hotel Shinjuku Granbell"
        ),
        ItineraryStopItem(
            id = "stop-5",
            dayNumber = 2,
            dateIso = "2026-11-12",
            startTime = "07:00",
            endTime = "09:30",
            title = "Fushimi Inari-taisha (Mil Torii)",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "伏見稲荷大社",
            latitude = 34.9671,
            longitude = 135.7727,
            notes = "JR Nara Line hasta estación Inari (5 min desde Kyoto Station)",
            photoLabels = listOf("Senbon Torii", "Mapa Sendero Inari"),
            linkedDocumentTitle = "Ruta Temprana por Fushimi Inari"
        ),
        ItineraryStopItem(
            id = "stop-6",
            dayNumber = 2,
            dateIso = "2026-11-12",
            startTime = "10:15",
            endTime = "12:30",
            title = "Kiyomizu-dera & Sannenzaka / Ninenzaka",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "清水寺 · 三年坂",
            latitude = 34.9949,
            longitude = 135.7850,
            notes = "Probar Matcha helado y Yatsuhashi en la cuesta histórica",
            photoLabels = listOf("Terraza de Madera Kiyomizu")
        )
    )

    fun sortAndDetectConflicts(items: List<ItineraryStopItem>): List<AnalyzedItineraryStop> {
        val sorted = items.sortedBy { toMinutes(it.startTime) }
        return sorted.mapIndexed { index, current ->
            val currentStart = toMinutes(current.startTime)
            val currentEnd = toMinutes(current.endTime)
            val overlapsWithAny = sorted.withIndex().any { (otherIndex, other) ->
                if (otherIndex == index) return@any false
                val otherStart = toMinutes(other.startTime)
                val otherEnd = toMinutes(other.endTime)
                currentStart < otherEnd && currentEnd > otherStart
            }
            AnalyzedItineraryStop(
                item = current,
                orderNumber = index + 1,
                hasTimeConflict = overlapsWithAny
            )
        }
    }

    fun extractCoordinatesFromGoogleMapsUrl(url: String): Pair<Double, Double>? {
        val atPattern = """@(-?\d+\.\d+),(-?\d+\.\d+)""".toRegex()
        val queryPattern = """[?&]q=(-?\d+\.\d+),(-?\d+\.\d+)""".toRegex()
        val match = atPattern.find(url) ?: queryPattern.find(url) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].toDoubleOrNull() ?: return null
        return lat to lng
    }

    private fun toMinutes(hhmm: String): Int {
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }
}

