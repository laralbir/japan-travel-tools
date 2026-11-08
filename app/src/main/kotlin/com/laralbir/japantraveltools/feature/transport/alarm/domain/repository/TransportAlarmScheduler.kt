package com.laralbir.japantraveltools.feature.transport.alarm.domain.repository

import com.laralbir.japantraveltools.feature.transport.alarm.domain.model.TransportAlarmSchedule

/**
 * Contract for scheduling, cancelling and synchronizing offline transport alarms with Android AlarmManager.
 */
interface TransportAlarmScheduler {

    /**
     * Schedules the provided list of alarms in the system.
     */
    fun scheduleAlarms(schedules: List<TransportAlarmSchedule>)

    /**
     * Cancels all scheduled alarms associated with the given transport segment ID.
     */
    fun cancelAlarmsForSegment(segmentId: String)

    /**
     * Cancels all scheduled transport alarms currently active in the system.
     */
    fun cancelAllAlarms()
}
