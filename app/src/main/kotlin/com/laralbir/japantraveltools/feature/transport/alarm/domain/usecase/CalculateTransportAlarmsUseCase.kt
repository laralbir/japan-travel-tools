package com.laralbir.japantraveltools.feature.transport.alarm.domain.usecase

import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import com.laralbir.japantraveltools.feature.transport.alarm.domain.model.TransportAlarmSchedule
import com.laralbir.japantraveltools.feature.transport.alarm.domain.model.TransportAlarmType
import com.laralbir.japantraveltools.feature.transport.domain.TransportType
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure domain use case responsible for calculating offline background alarm schedules
 * for a given [TransportSegmentItem] based on temporal thresholds and offline-first business rules.
 */
class CalculateTransportAlarmsUseCase {

    companion object {
        const val MILLIS_24_HOURS = 24 * 3600 * 1000L
        const val MILLIS_2_HOURS = 2 * 3600 * 1000L
        const val MILLIS_60_MINUTES = 60 * 60 * 1000L
        const val MILLIS_15_MINUTES = 15 * 60 * 1000L
        const val MILLIS_5_MINUTES = 5 * 60 * 1000L

        operator fun invoke(segment: TransportSegmentItem, currentTimeMillis: Long): List<TransportAlarmSchedule> =
            CalculateTransportAlarmsUseCase().execute(segment, currentTimeMillis)
    }

    /**
     * Calculates the list of actionable future alarms for [segment] relative to [currentTimeMillis].
     * Never returns alarms scheduled in the past or exactly at [currentTimeMillis].
     */
    fun execute(segment: TransportSegmentItem, currentTimeMillis: Long): List<TransportAlarmSchedule> {
        val depEpochMillis = parseIsoToEpochMillis(segment.departureTimeIso, segment.departureZoneId)
            ?: return emptyList()

        val candidateAlarms = mutableListOf<TransportAlarmSchedule>()

        when (segment.type) {
            TransportType.FLIGHT -> {
                // 1. FLIGHT_CHECK_IN_24H
                candidateAlarms.add(
                    buildSchedule(
                        segment = segment,
                        alarmType = TransportAlarmType.FLIGHT_CHECK_IN_24H,
                        triggerEpochMillis = depEpochMillis - MILLIS_24_HOURS,
                        title = "✈️ Check-in Abierto: ${segment.operatorName} ${segment.lineOrNumber}".trim(),
                        summary = "Facturación online disponible 24h antes del vuelo hacia ${segment.arrivalName}."
                    )
                )

                // 2. FLIGHT_CHECK_IN_2H
                candidateAlarms.add(
                    buildSchedule(
                        segment = segment,
                        alarmType = TransportAlarmType.FLIGHT_CHECK_IN_2H,
                        triggerEpochMillis = depEpochMillis - MILLIS_2_HOURS,
                        title = "✈️ Cierre de Facturación (2h): ${segment.operatorName} ${segment.lineOrNumber}".trim(),
                        summary = "Cierre de mostradores y facturación de equipaje en ${segment.departureTerminalOrPlatform} en 2 horas."
                    )
                )

                // 3. DEPARTURE_60M
                candidateAlarms.add(
                    buildSchedule(
                        segment = segment,
                        alarmType = TransportAlarmType.DEPARTURE_60M,
                        triggerEpochMillis = depEpochMillis - MILLIS_60_MINUTES,
                        title = "✈️ Salida en 60 min: ${segment.operatorName} ${segment.lineOrNumber}".trim(),
                        summary = "Salida en 60 min desde ${segment.departureTerminalOrPlatform} hacia ${segment.arrivalName}."
                    )
                )

                // 4. DEPARTURE_15M
                candidateAlarms.add(
                    buildSchedule(
                        segment = segment,
                        alarmType = TransportAlarmType.DEPARTURE_15M,
                        triggerEpochMillis = depEpochMillis - MILLIS_15_MINUTES,
                        title = "✈️ Salida Inminente (15 min): ${segment.operatorName} ${segment.lineOrNumber}".trim(),
                        summary = "Salida en 15 min desde ${segment.departureTerminalOrPlatform}. ${segment.seatOrCarInfo}".trim()
                    )
                )
            }

            TransportType.TRAIN_SHINKANSEN,
            TransportType.SUBWAY_METRO,
            TransportType.BUS -> {
                val emoji = transportEmoji(segment.type)

                // 1. DEPARTURE_60M
                candidateAlarms.add(
                    buildSchedule(
                        segment = segment,
                        alarmType = TransportAlarmType.DEPARTURE_60M,
                        triggerEpochMillis = depEpochMillis - MILLIS_60_MINUTES,
                        title = "$emoji Salida en 60 min: ${segment.operatorName} ${segment.lineOrNumber}".trim(),
                        summary = "Salida en 60 min desde ${segment.departureTerminalOrPlatform} hacia ${segment.arrivalName}."
                    )
                )

                // 2. DEPARTURE_15M
                candidateAlarms.add(
                    buildSchedule(
                        segment = segment,
                        alarmType = TransportAlarmType.DEPARTURE_15M,
                        triggerEpochMillis = depEpochMillis - MILLIS_15_MINUTES,
                        title = "$emoji Salida Inminente (15 min): ${segment.operatorName} ${segment.lineOrNumber}".trim(),
                        summary = "Salida en 15 min desde ${segment.departureTerminalOrPlatform}. ${segment.seatOrCarInfo}".trim()
                    )
                )

                // 3. ARRIVAL_APPROACHING_5M (if arrival time is valid)
                val arrEpochMillis = parseIsoToEpochMillis(segment.arrivalTimeIso, segment.arrivalZoneId)
                if (arrEpochMillis != null) {
                    val arrivalPlatform = segment.arrivalTerminalOrPlatform.ifBlank {
                        segment.departureTerminalOrPlatform
                    }
                    candidateAlarms.add(
                        TransportAlarmSchedule(
                            segmentId = segment.id,
                            alarmType = TransportAlarmType.ARRIVAL_APPROACHING_5M,
                            triggerTimeUtcIso = Instant.ofEpochMilli(arrEpochMillis - MILLIS_5_MINUTES).toString(),
                            triggerTimeEpochMillis = arrEpochMillis - MILLIS_5_MINUTES,
                            transportType = segment.type,
                            title = "$emoji Próxima Llegada (5 min): ${segment.arrivalName}".trim(),
                            summary = "Llegada inminente a ${segment.arrivalName} en 5 min. ¡Prepara tu equipaje!",
                            platform = arrivalPlatform,
                            carAndSeat = segment.seatOrCarInfo,
                            qrCodePayload = segment.barcodeMessage
                        )
                    )
                }
            }
        }

        // Filter out any alarms whose trigger time is in the past or exactly now
        return candidateAlarms.filter { it.triggerTimeEpochMillis > currentTimeMillis }
    }

    operator fun invoke(segment: TransportSegmentItem, currentTimeMillis: Long): List<TransportAlarmSchedule> =
        execute(segment, currentTimeMillis)

    private fun buildSchedule(
        segment: TransportSegmentItem,
        alarmType: TransportAlarmType,
        triggerEpochMillis: Long,
        title: String,
        summary: String
    ): TransportAlarmSchedule {
        return TransportAlarmSchedule(
            segmentId = segment.id,
            alarmType = alarmType,
            triggerTimeUtcIso = Instant.ofEpochMilli(triggerEpochMillis).toString(),
            triggerTimeEpochMillis = triggerEpochMillis,
            transportType = segment.type,
            title = title,
            summary = summary,
            platform = segment.departureTerminalOrPlatform,
            carAndSeat = segment.seatOrCarInfo,
            qrCodePayload = segment.barcodeMessage
        )
    }

    private fun transportEmoji(type: TransportType): String = when (type) {
        TransportType.FLIGHT -> "✈️"
        TransportType.TRAIN_SHINKANSEN -> "🚄"
        TransportType.SUBWAY_METRO -> "🚇"
        TransportType.BUS -> "🚌"
    }

    private fun parseIsoToEpochMillis(isoString: String?, zoneIdString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        val zone = runCatching {
            if (!zoneIdString.isNullOrBlank()) ZoneId.of(zoneIdString) else ZoneId.systemDefault()
        }.getOrElse { ZoneId.of("UTC") }

        runCatching {
            return Instant.parse(isoString).toEpochMilli()
        }
        runCatching {
            return ZonedDateTime.parse(isoString).toInstant().toEpochMilli()
        }
        runCatching {
            return OffsetDateTime.parse(isoString).toInstant().toEpochMilli()
        }
        runCatching {
            val ldt = LocalDateTime.parse(isoString)
            return ZonedDateTime.of(ldt, zone).toInstant().toEpochMilli()
        }
        return null
    }
}
