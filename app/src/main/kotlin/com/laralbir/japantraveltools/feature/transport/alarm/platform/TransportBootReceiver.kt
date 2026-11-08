package com.laralbir.japantraveltools.feature.transport.alarm.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.laralbir.japantraveltools.core.portability.SharedTravelPlanStore
import com.laralbir.japantraveltools.feature.transport.alarm.domain.repository.TransportAlarmScheduler
import com.laralbir.japantraveltools.feature.transport.alarm.domain.usecase.CalculateTransportAlarmsUseCase

/**
 * BroadcastReceiver triggered when the device boots or package is updated.
 * Automatically restores and reschedules all active future transport alarms from [SharedTravelPlanStore].
 */
class TransportBootReceiver : BroadcastReceiver() {

    companion object {
        fun rescheduleAllAlarms(
            context: Context? = null,
            scheduler: TransportAlarmScheduler? = null,
            calculateAlarms: CalculateTransportAlarmsUseCase = CalculateTransportAlarmsUseCase(),
            currentTimeMillis: Long = System.currentTimeMillis()
        ): Int {
            if (context != null) {
                runCatching { SharedTravelPlanStore.init(context) }
            }
            val activeScheduler = scheduler ?: context?.let { AndroidTransportAlarmScheduler(it) } ?: return 0

            val segments = SharedTravelPlanStore.allPlansState.value
                .flatMap { it.transportSegments }
                .ifEmpty { SharedTravelPlanStore.planState.value.transportSegments }
                .distinctBy { it.id }

            val futureAlarms = segments.flatMap { segment ->
                calculateAlarms(segment, currentTimeMillis)
            }

            if (futureAlarms.isNotEmpty()) {
                activeScheduler.scheduleAlarms(futureAlarms)
            }
            return futureAlarms.size
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            rescheduleAllAlarms(context)
        }
    }
}
