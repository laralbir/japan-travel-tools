package com.laralbir.japantraveltools.feature.transport

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class TransportAlertSeverity {
    INFO,
    WARNING,
    URGENT
}

enum class TransportAlertType {
    CHECK_IN_APPROACHING,
    CHECK_IN_OPEN,
    BOARDING_APPROACHING,
    BOARDING_NOW,
    DEPARTURE_IMMINENT
}

data class TransportAlert(
    val segmentId: String,
    val type: TransportAlertType,
    val severity: TransportAlertSeverity,
    val titleEs: String,
    val titleEn: String,
    val messageEs: String,
    val messageEn: String,
    val minutesUntilEvent: Long,
    val eventTimeIso: String,
    val targetGateOrCounter: String
) {
    val uniqueKey: String get() = "${segmentId}_${type.name}"
}

object TransportAlertEvaluator {

    fun evaluateSegmentAlerts(
        segments: List<TransportSegmentItem>,
        now: ZonedDateTime,
        dismissedAlertKeys: Set<String> = emptySet()
    ): List<TransportAlert> {
        return segments.mapNotNull { seg ->
            evaluateSegmentAlert(seg, now)
        }.filter { alert ->
            alert.uniqueKey !in dismissedAlertKeys
        }.sortedWith(
            compareByDescending<TransportAlert> { it.severity == TransportAlertSeverity.URGENT }
                .thenBy { it.minutesUntilEvent }
        )
    }

    fun evaluateSegmentAlert(
        segment: TransportSegmentItem,
        now: ZonedDateTime
    ): TransportAlert? {
        val zone = runCatching { ZoneId.of(segment.departureZoneId) }.getOrElse { ZoneId.systemDefault() }
        val nowInSegmentZone = now.withZoneSameInstant(zone)

        val depLocal = runCatching { LocalDateTime.parse(segment.departureTimeIso) }.getOrNull() ?: return null
        val depZdt = ZonedDateTime.of(depLocal, zone)

        // If the departure is more than 30 minutes in the past, no alert
        if (nowInSegmentZone.isAfter(depZdt.plusMinutes(30))) {
            return null
        }

        val checkInZdt = segment.checkInTime?.let {
            runCatching { ZonedDateTime.of(LocalDateTime.parse(it), zone) }.getOrNull()
        } ?: if (segment.type == TransportMode.FLIGHT) {
            depZdt.minusHours(3)
        } else null

        val boardingZdt = segment.boardingTime?.let {
            runCatching { ZonedDateTime.of(LocalDateTime.parse(it), zone) }.getOrNull()
        } ?: when (segment.type) {
            TransportMode.FLIGHT -> depZdt.minusMinutes(50)
            TransportMode.TRAIN_SHINKANSEN -> depZdt.minusMinutes(12)
            else -> depZdt.minusMinutes(10)
        }

        val minutesUntilDep = Duration.between(nowInSegmentZone, depZdt).toMinutes()

        // 1. DEPARTURE_IMMINENT (<= 20 mins before departure, past boarding)
        val isPastBoarding = boardingZdt.let { nowInSegmentZone.isAfter(it) || nowInSegmentZone.isEqual(it) }
        if (isPastBoarding && minutesUntilDep in 0..20) {
            val titleEs = "🚨 Salida Inminente: ${segment.operatorName} ${segment.lineOrNumber}"
            val titleEn = "🚨 Imminent Departure: ${segment.operatorName} ${segment.lineOrNumber}"
            val msgEs = "El vuelo parte en $minutesUntilDep min desde ${segment.departureTerminalOrPlatform}. ¡Cierre de puertas inmediato!"
            val msgEn = "Departing in $minutesUntilDep min from ${segment.departureTerminalOrPlatform}. Gate closing now!"
            return TransportAlert(
                segmentId = segment.id,
                type = TransportAlertType.DEPARTURE_IMMINENT,
                severity = TransportAlertSeverity.URGENT,
                titleEs = titleEs,
                titleEn = titleEn,
                messageEs = msgEs,
                messageEn = msgEn,
                minutesUntilEvent = maxOf(0L, minutesUntilDep),
                eventTimeIso = segment.departureTimeIso,
                targetGateOrCounter = segment.departureTerminalOrPlatform
            )
        }

        // 2. BOARDING_NOW (<= 15 mins before boarding or during boarding window)
        val minutesUntilBoarding = Duration.between(nowInSegmentZone, boardingZdt).toMinutes()
        if (minutesUntilBoarding in -15..15) {
            val titleEs = "🚨 ¡Embarque en Curso! ${segment.operatorName} ${segment.lineOrNumber}"
            val titleEn = "🚨 Boarding Now! ${segment.operatorName} ${segment.lineOrNumber}"
            val minsText = if (minutesUntilBoarding > 0) "en $minutesUntilBoarding min" else "¡AHORA!"
            val msgEs = "Embarque $minsText en ${segment.departureTerminalOrPlatform}. ${segment.seatOrCarInfo}"
            val msgEn = "Boarding $minsText at ${segment.departureTerminalOrPlatform}. ${segment.seatOrCarInfo}"
            return TransportAlert(
                segmentId = segment.id,
                type = TransportAlertType.BOARDING_NOW,
                severity = TransportAlertSeverity.URGENT,
                titleEs = titleEs,
                titleEn = titleEn,
                messageEs = msgEs,
                messageEn = msgEn,
                minutesUntilEvent = maxOf(0L, minutesUntilBoarding),
                eventTimeIso = segment.boardingTime ?: boardingZdt.toLocalDateTime().toString(),
                targetGateOrCounter = segment.departureTerminalOrPlatform
            )
        }

        // 3. BOARDING_APPROACHING (16..45 mins before boarding)
        if (minutesUntilBoarding in 16..45) {
            val titleEs = "⚠️ Embarque próximo: ${segment.operatorName} ${segment.lineOrNumber}"
            val titleEn = "⚠️ Boarding Soon: ${segment.operatorName} ${segment.lineOrNumber}"
            val msgEs = "El embarque comienza en $minutesUntilBoarding min en ${segment.departureTerminalOrPlatform}. ${segment.seatOrCarInfo}"
            val msgEn = "Boarding starts in $minutesUntilBoarding min at ${segment.departureTerminalOrPlatform}. ${segment.seatOrCarInfo}"
            return TransportAlert(
                segmentId = segment.id,
                type = TransportAlertType.BOARDING_APPROACHING,
                severity = TransportAlertSeverity.WARNING,
                titleEs = titleEs,
                titleEn = titleEn,
                messageEs = msgEs,
                messageEn = msgEn,
                minutesUntilEvent = minutesUntilBoarding,
                eventTimeIso = segment.boardingTime ?: boardingZdt.toLocalDateTime().toString(),
                targetGateOrCounter = segment.departureTerminalOrPlatform
            )
        }

        // 4. CHECK_IN_OPEN (checkIn has passed, but boarding is still > 45 mins away)
        if (checkInZdt != null && (nowInSegmentZone.isAfter(checkInZdt) || nowInSegmentZone.isEqual(checkInZdt))) {
            if (minutesUntilBoarding > 45) {
                val titleEs = "ℹ️ Facturación abierta: ${segment.operatorName} ${segment.lineOrNumber}"
                val titleEn = "ℹ️ Check-in Open: ${segment.operatorName} ${segment.lineOrNumber}"
                val msgEs = "Mostrador abierto en ${segment.departureTerminalOrPlatform}. Entrega tu equipaje antes del cierre."
                val msgEn = "Counters open at ${segment.departureTerminalOrPlatform}. Drop your bags before closing."
                return TransportAlert(
                    segmentId = segment.id,
                    type = TransportAlertType.CHECK_IN_OPEN,
                    severity = TransportAlertSeverity.WARNING,
                    titleEs = titleEs,
                    titleEn = titleEn,
                    messageEs = msgEs,
                    messageEn = msgEn,
                    minutesUntilEvent = 0L,
                    eventTimeIso = segment.checkInTime ?: checkInZdt.toLocalDateTime().toString(),
                    targetGateOrCounter = segment.departureTerminalOrPlatform
                )
            }
        }

        // 5. CHECK_IN_APPROACHING (1..120 mins before checkIn)
        if (checkInZdt != null) {
            val minutesUntilCheckIn = Duration.between(nowInSegmentZone, checkInZdt).toMinutes()
            if (minutesUntilCheckIn in 1..120) {
                val titleEs = "🕒 Facturación próxima: ${segment.operatorName} ${segment.lineOrNumber}"
                val titleEn = "🕒 Check-in Soon: ${segment.operatorName} ${segment.lineOrNumber}"
                val msgEs = "La facturación abre en $minutesUntilCheckIn min en ${segment.departureTerminalOrPlatform}."
                val msgEn = "Check-in opens in $minutesUntilCheckIn min at ${segment.departureTerminalOrPlatform}."
                return TransportAlert(
                    segmentId = segment.id,
                    type = TransportAlertType.CHECK_IN_APPROACHING,
                    severity = TransportAlertSeverity.WARNING,
                    titleEs = titleEs,
                    titleEn = titleEn,
                    messageEs = msgEs,
                    messageEn = msgEn,
                    minutesUntilEvent = minutesUntilCheckIn,
                    eventTimeIso = segment.checkInTime ?: checkInZdt.toLocalDateTime().toString(),
                    targetGateOrCounter = segment.departureTerminalOrPlatform
                )
            }
        }

        // 6. For trains / buses without check-in: approaching departure within 30 min
        if (segment.type != TransportMode.FLIGHT && minutesUntilDep in 1..30) {
            val titleEs = "🚄 Acceso al Andén: ${segment.operatorName} ${segment.lineOrNumber}"
            val titleEn = "🚄 Track Arrival: ${segment.operatorName} ${segment.lineOrNumber}"
            val msgEs = "Salida en $minutesUntilDep min desde ${segment.departureTerminalOrPlatform}. ${segment.seatOrCarInfo}"
            val msgEn = "Departing in $minutesUntilDep min from ${segment.departureTerminalOrPlatform}. ${segment.seatOrCarInfo}"
            return TransportAlert(
                segmentId = segment.id,
                type = TransportAlertType.BOARDING_APPROACHING,
                severity = TransportAlertSeverity.WARNING,
                titleEs = titleEs,
                titleEn = titleEn,
                messageEs = msgEs,
                messageEn = msgEn,
                minutesUntilEvent = minutesUntilDep,
                eventTimeIso = segment.departureTimeIso,
                targetGateOrCounter = segment.departureTerminalOrPlatform
            )
        }

        return null
    }
}
