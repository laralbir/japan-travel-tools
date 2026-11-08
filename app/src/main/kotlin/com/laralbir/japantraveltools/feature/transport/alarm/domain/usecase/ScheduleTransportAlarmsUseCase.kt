package com.laralbir.japantraveltools.feature.transport.alarm.domain.usecase

import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import com.laralbir.japantraveltools.feature.transport.alarm.domain.repository.TransportAlarmScheduler

/**
 * Orchestrator use case that calculates actionable future alarms for a segment
 * and updates [TransportAlarmScheduler] by cancelling old alarms and scheduling new ones.
 */
class ScheduleTransportAlarmsUseCase(
    private val scheduler: TransportAlarmScheduler,
    private val calculateAlarms: CalculateTransportAlarmsUseCase = CalculateTransportAlarmsUseCase()
) {

    /**
     * Calculates alarms for [segment] at [currentTimeMillis] and registers them with [scheduler].
     */
    operator fun invoke(segment: TransportSegmentItem, currentTimeMillis: Long = System.currentTimeMillis()) {
        val alarms = calculateAlarms(segment, currentTimeMillis)
        scheduler.cancelAlarmsForSegment(segment.id)
        if (alarms.isNotEmpty()) {
            scheduler.scheduleAlarms(alarms)
        }
    }
}
