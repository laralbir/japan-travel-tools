package com.laralbir.japantraveltools.feature.transport.alarm

import com.laralbir.japantraveltools.feature.transport.alarm.platform.TransportAlarmReceiver
import org.junit.Assert.assertEquals
import org.junit.Test

class TransportAlarmReceiverTest {

    @Test
    fun testActionAndChannelConstants() {
        assertEquals("com.laralbir.japantraveltools.action.TRANSPORT_ALARM", TransportAlarmReceiver.ACTION_TRANSPORT_ALARM)
        assertEquals("japan_travel_transport_alarms", TransportAlarmReceiver.CHANNEL_ID)
        assertEquals("Transport Alerts", TransportAlarmReceiver.CHANNEL_NAME)
        assertEquals("com.laralbir.japantraveltools.action.VIEW_TICKET", TransportAlarmReceiver.ACTION_VIEW_TICKET)
    }

    @Test
    fun testExtraKeysConstants() {
        assertEquals("extra_segment_id", TransportAlarmReceiver.EXTRA_SEGMENT_ID)
        assertEquals("extra_alarm_type", TransportAlarmReceiver.EXTRA_ALARM_TYPE)
        assertEquals("extra_trigger_epoch", TransportAlarmReceiver.EXTRA_TRIGGER_EPOCH)
        assertEquals("extra_trigger_utc_iso", TransportAlarmReceiver.EXTRA_TRIGGER_UTC_ISO)
        assertEquals("extra_transport_type", TransportAlarmReceiver.EXTRA_TRANSPORT_TYPE)
        assertEquals("extra_title", TransportAlarmReceiver.EXTRA_TITLE)
        assertEquals("extra_summary", TransportAlarmReceiver.EXTRA_SUMMARY)
        assertEquals("extra_platform", TransportAlarmReceiver.EXTRA_PLATFORM)
        assertEquals("extra_car_and_seat", TransportAlarmReceiver.EXTRA_CAR_AND_SEAT)
        assertEquals("extra_qr_code", TransportAlarmReceiver.EXTRA_QR_CODE)
        assertEquals("extra_view_ticket_qr", TransportAlarmReceiver.EXTRA_VIEW_TICKET_QR)
    }
}
