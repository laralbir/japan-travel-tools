package com.laralbir.japantraveltools.feature.weather

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WeatherSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        SharedWeatherForecastStore.init(applicationContext)
        val cities = SharedWeatherForecastStore.citiesState.value
        if (cities.isEmpty()) return Result.success()

        var anySuccess = false
        for (city in cities) {
            val forecastResult = OpenMeteoWeatherUseCase.fetchCoordinatesForecastOnline(
                cityKey = city.id,
                latitude = city.latitude,
                longitude = city.longitude
            )
            forecastResult.onSuccess { freshDays ->
                SharedWeatherForecastStore.updateCityForecasts(city.id, freshDays)
                anySuccess = true
            }
        }

        return if (anySuccess) Result.success() else Result.retry()
    }
}

object WeatherSyncScheduler {
    private const val WORK_NAME = "japan_travel_weather_sync"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }
}
