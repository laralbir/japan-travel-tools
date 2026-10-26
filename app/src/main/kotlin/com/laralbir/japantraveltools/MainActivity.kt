package com.laralbir.japantraveltools

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.laralbir.japantraveltools.core.portability.SharedTravelPlanStore
import com.laralbir.japantraveltools.feature.currency.CurrencySyncScheduler
import com.laralbir.japantraveltools.feature.currency.SharedCurrencyRateStore
import com.laralbir.japantraveltools.feature.documents.SharedDocumentsAndGuidesStore
import com.laralbir.japantraveltools.feature.transport.TransportAlertNotificationHelper
import com.laralbir.japantraveltools.feature.weather.SharedWeatherForecastStore
import com.laralbir.japantraveltools.ui.theme.JapanTravelToolsTheme

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    SharedTravelPlanStore.init(this)
    SharedDocumentsAndGuidesStore.init(this)
    SharedWeatherForecastStore.init(this)
    SharedCurrencyRateStore.init(this)
    CurrencySyncScheduler.schedule(this)
    TransportAlertNotificationHelper.ensureChannelCreated(this)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
      }
    }

    enableEdgeToEdge()
    setContent {
      JapanTravelToolsTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
