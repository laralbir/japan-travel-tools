package com.laralbir.japantraveltools.feature.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TransportAlertNotificationHelperTest {

    @Test
    fun `channel constants are defined correctly`() {
        assertEquals("transport_alerts", TransportAlertNotificationHelper.CHANNEL_ID)
        assertEquals("Avisos de Transporte y Vuelos", TransportAlertNotificationHelper.CHANNEL_NAME)
    }

    @Test
    fun `buildNotificationSettingsIntent creates valid intent`() {
        val packageName = "com.laralbir.japantraveltools"
        val intent = TransportAlertNotificationHelper.buildNotificationSettingsIntent(packageName)
        assertNotNull(intent)
    }

    @Test
    fun `isPostNotificationsPermissionRequired returns boolean without exception`() {
        val required = TransportAlertNotificationHelper.isPostNotificationsPermissionRequired()
        assertNotNull(required)
    }
}
