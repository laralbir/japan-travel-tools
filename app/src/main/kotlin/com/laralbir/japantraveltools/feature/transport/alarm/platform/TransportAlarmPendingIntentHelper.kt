package com.laralbir.japantraveltools.feature.transport.alarm.platform

import com.laralbir.japantraveltools.feature.transport.alarm.domain.model.TransportAlarmType

/**
 * Helper utility to generate deterministic, unique and positive request codes for AlarmManager PendingIntents.
 */
object TransportAlarmPendingIntentHelper {

    /**
     * Generates a deterministic positive 31-bit integer request code for a given segment and alarm type.
     */
    fun generateRequestCode(segmentId: String, alarmType: TransportAlarmType): Int =
        (segmentId.hashCode() xor (alarmType.ordinal * 31)) and 0x7FFFFFFF
}
