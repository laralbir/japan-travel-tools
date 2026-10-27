package com.laralbir.japantraveltools.feature.currency

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class CurrencySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        SharedCurrencyRateStore.init(applicationContext)
        val fetchResult = FrankfurterJsonParser.fetchFromFrankfurterApi()
        return if (fetchResult.isSuccess) {
            val fresh = fetchResult.getOrThrow()
            SharedCurrencyRateStore.updateSnapshot(fresh)
            Result.success()
        } else {
            Result.retry()
        }
    }
}

object CurrencySyncScheduler {
    private const val WORK_NAME = "japan_travel_currency_sync"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<CurrencySyncWorker>(
            repeatInterval = 24,
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
