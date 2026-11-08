package com.laralbir.japantraveltools.feature.transport.alarm.domain.model

import com.laralbir.japantraveltools.feature.transport.domain.TransportType

/**
 * Types of offline background alarms and notifications scheduled via AlarmManager.
 */
enum class TransportAlarmType {
    FLIGHT_CHECK_IN_24H,
    FLIGHT_CHECK_IN_2H,
    DEPARTURE_60M,
    DEPARTURE_15M,
    ARRIVAL_APPROACHING_5M
}

/**
 * Domain entity representing a scheduled background alarm for a transport segment.
 */
data class TransportAlarmSchedule(
    val segmentId: String,
    val alarmType: TransportAlarmType,
    val triggerTimeUtcIso: String,
    val triggerTimeEpochMillis: Long,
    val transportType: TransportType,
    val title: String,
    val summary: String,
    val platform: String,
    val carAndSeat: String,
    val qrCodePayload: String?
)
