package com.laralbir.japantraveltools.feature.transport

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class TransportMode(val labelEn: String, val labelEs: String, val badge: String) {
    FLIGHT("Flight", "Vuelo", "FLIGHT"),
    TRAIN_SHINKANSEN("Shinkansen / Express", "Shinkansen / Expreso", "SHINKANSEN"),
    SUBWAY_METRO("Subway / Metro", "Metro Urbano", "METRO"),
    BUS("Highway / City Bus", "Autobús", "BUS")
}

data class TransportSegmentItem(
    val id: String,
    val order: Int,
    val type: TransportMode,
    val operatorName: String,
    val lineOrNumber: String,
    val departureCode: String,
    val departureName: String,
    val departureZoneId: String,
    val departureTerminalOrPlatform: String,
    val checkInTime: String? = null,
    val boardingTime: String? = null,
    val departureTimeIso: String,
    val arrivalCode: String,
    val arrivalName: String,
    val arrivalZoneId: String,
    val arrivalTerminalOrPlatform: String,
    val arrivalTimeIso: String,
    val seatOrCarInfo: String,
    val stationExitOrBelt: String = "",
    val passOrPaymentBadge: String = "Included",
    val departureTimeUtcIso: String? = null,
    val arrivalTimeUtcIso: String? = null
)

data class ConnectionCalculation(
    val fromCode: String,
    val toCode: String,
    val totalConnectionMinutes: Long,
    val minutesUntilNextBoardingOrDeparture: Long,
    val isTightConnection: Boolean,
    val transferSummary: String
)

object TransportPlannerUseCase {

    fun createEmptySegment(
        id: String = "seg-${System.currentTimeMillis()}",
        order: Int = 1,
        initialMode: TransportMode = TransportMode.TRAIN_SHINKANSEN,
        zoneId: String = "Asia/Tokyo"
    ): TransportSegmentItem = TransportSegmentItem(
        id = id,
        order = order,
        type = initialMode,
        operatorName = "",
        lineOrNumber = "",
        departureCode = "",
        departureName = "",
        departureZoneId = zoneId,
        departureTerminalOrPlatform = "",
        checkInTime = null,
        boardingTime = null,
        departureTimeIso = "",
        arrivalCode = "",
        arrivalName = "",
        arrivalZoneId = zoneId,
        arrivalTerminalOrPlatform = "",
        arrivalTimeIso = "",
        seatOrCarInfo = "",
        stationExitOrBelt = "",
        passOrPaymentBadge = ""
    )

    fun defaultSampleSegments(): List<TransportSegmentItem> = listOf(
        TransportSegmentItem(
            id = "seg-flight-1",
            order = 1,
            type = TransportMode.FLIGHT,
            operatorName = "Qatar Airways",
            lineOrNumber = "QR150 · PNR JPN26X",
            departureCode = "MAD",
            departureName = "Madrid Barajas",
            departureZoneId = "Europe/Madrid",
            departureTerminalOrPlatform = "Terminal 4S · Mostrador 810",
            checkInTime = "2026-11-10T12:30",
            boardingTime = "2026-11-10T14:40",
            departureTimeIso = "2026-11-10T15:30",
            arrivalCode = "DOH",
            arrivalName = "Doha Hamad Intl",
            arrivalZoneId = "Asia/Qatar",
            arrivalTerminalOrPlatform = "Terminal 1",
            arrivalTimeIso = "2026-11-10T23:45",
            seatOrCarInfo = "Puerta S22 · Asiento 18A (Ventana)",
            stationExitOrBelt = "Tránsito Internacional",
            passOrPaymentBadge = "Facturado a HND"
        ),
        TransportSegmentItem(
            id = "seg-flight-2",
            order = 2,
            type = TransportMode.FLIGHT,
            operatorName = "Qatar Airways",
            lineOrNumber = "QR806 · PNR JPN26X",
            departureCode = "DOH",
            departureName = "Doha Hamad Intl",
            departureZoneId = "Asia/Qatar",
            departureTerminalOrPlatform = "Terminal 1 · Zona C",
            checkInTime = null,
            boardingTime = "2026-11-11T01:15",
            departureTimeIso = "2026-11-11T02:15",
            arrivalCode = "HND",
            arrivalName = "Tokyo Haneda (羽田空港)",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "Terminal 3 Internacional",
            arrivalTimeIso = "2026-11-11T17:55",
            seatOrCarInfo = "Puerta C14 · Asiento 24K (+1 Día)",
            stationExitOrBelt = "Cinta Equipaje #4 · Mostrar QR Visit Japan",
            passOrPaymentBadge = "Boleto QR Vinculado"
        ),
        TransportSegmentItem(
            id = "seg-train-3",
            order = 3,
            type = TransportMode.TRAIN_SHINKANSEN,
            operatorName = "JR Central · Tokaido Shinkansen",
            lineOrNumber = "Nozomi #21 (のぞみ21号)",
            departureCode = "TYO",
            departureName = "Tokyo Station (東京駅)",
            departureZoneId = "Asia/Tokyo",
            departureTerminalOrPlatform = "Andén 16 (Track 16)",
            checkInTime = "2026-11-14T08:40",
            boardingTime = "2026-11-14T08:52",
            departureTimeIso = "2026-11-14T09:00",
            arrivalCode = "KYO",
            arrivalName = "Kyoto Station (京都駅)",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "Andén 13 (Track 13)",
            arrivalTimeIso = "2026-11-14T11:15",
            seatOrCarInfo = "Coche 7 · Asiento 12E (Vista Monte Fuji) · Maleta Grande OK",
            stationExitOrBelt = "Salida Central / Transfer Metro Karasuma",
            passOrPaymentBadge = "JR Pass / SmartEX QR"
        ),
        TransportSegmentItem(
            id = "seg-metro-4",
            order = 4,
            type = TransportMode.SUBWAY_METRO,
            operatorName = "Tokyo Metro",
            lineOrNumber = "Ginza Line (銀座線 · Color Naranja)",
            departureCode = "G19",
            departureName = "Asakusa (浅草)",
            departureZoneId = "Asia/Tokyo",
            departureTerminalOrPlatform = "Andén 1 (Dir. Shibuya)",
            boardingTime = "2026-11-11T10:35",
            departureTimeIso = "2026-11-11T10:38",
            arrivalCode = "G09",
            arrivalName = "Ginza (銀座)",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "Andén 1",
            arrivalTimeIso = "2026-11-11T10:57",
            seatOrCarInfo = "Subir en Vagón 4 (Mejor para Salida A4)",
            stationExitOrBelt = "Tomar Salida A4 (Exit A4)",
            passOrPaymentBadge = "Suica / Pasmo IC (¥210)"
        ),
        TransportSegmentItem(
            id = "seg-bus-5",
            order = 5,
            type = TransportMode.BUS,
            operatorName = "Fuji Express Highway Bus",
            lineOrNumber = "Bus #1107 Shinjuku ➔ Kawaguchiko",
            departureCode = "BUSTA",
            departureName = "Busta Shinjuku 4F (バスタ新宿)",
            departureZoneId = "Asia/Tokyo",
            departureTerminalOrPlatform = "Dársena B4 (Bus Bay B4)",
            boardingTime = "2026-11-13T07:25",
            departureTimeIso = "2026-11-13T07:35",
            arrivalCode = "KWG",
            arrivalName = "Kawaguchiko Station (河口湖駅)",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "Parada 1",
            arrivalTimeIso = "2026-11-13T09:20",
            seatOrCarInfo = "Asiento 3A (Reservado)",
            stationExitOrBelt = "Frente a oficina turística Monte Fuji",
            passOrPaymentBadge = "Reserva HighwayBus PDF"
        )
    )

    fun calculateSegmentDurationMinutes(segment: TransportSegmentItem): Long {
        val depLocal = LocalDateTime.parse(segment.departureTimeIso)
        val arrLocal = LocalDateTime.parse(segment.arrivalTimeIso)
        val depZdt = ZonedDateTime.of(depLocal, ZoneId.of(segment.departureZoneId))
        val arrZdt = ZonedDateTime.of(arrLocal, ZoneId.of(segment.arrivalZoneId))
        return Duration.between(depZdt, arrZdt).toMinutes()
    }

    fun calculateConnection(
        first: TransportSegmentItem,
        second: TransportSegmentItem
    ): ConnectionCalculation {
        val arrZdt = ZonedDateTime.of(
            LocalDateTime.parse(first.arrivalTimeIso),
            ZoneId.of(first.arrivalZoneId)
        )
        val nextDepZdt = ZonedDateTime.of(
            LocalDateTime.parse(second.departureTimeIso),
            ZoneId.of(second.departureZoneId)
        )
        val nextBoardingZdt = second.boardingTime?.let {
            ZonedDateTime.of(LocalDateTime.parse(it), ZoneId.of(second.departureZoneId))
        } ?: nextDepZdt

        val totalMinutes = Duration.between(arrZdt, nextDepZdt).toMinutes()
        val untilBoardingMinutes = Duration.between(arrZdt, nextBoardingZdt).toMinutes()

        val tightThreshold = if (first.type == TransportMode.FLIGHT && second.type == TransportMode.FLIGHT) {
            60L
        } else {
            10L
        }

        return ConnectionCalculation(
            fromCode = first.arrivalCode,
            toCode = second.departureCode,
            totalConnectionMinutes = totalMinutes,
            minutesUntilNextBoardingOrDeparture = untilBoardingMinutes,
            isTightConnection = untilBoardingMinutes < tightThreshold,
            transferSummary = "${first.arrivalTerminalOrPlatform} ➔ ${second.departureTerminalOrPlatform}"
        )
    }

    fun formatDurationHoursMinutes(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}

