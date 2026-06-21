package com.ods.dashboard.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ods.dashboard.data.ConnectionRepository
import com.ods.dashboard.widget.OdsWidget
import java.util.concurrent.TimeUnit

/**
 * Periodic, battery-friendly status refresh. Updates the DataStore cache, then nudges
 * the Glance widget to redraw from it. Runs ~every 15 minutes (WorkManager minimum)
 * and only when the network is connected.
 */
class StatusRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            ConnectionRepository(applicationContext).refreshAll()
            OdsWidget.refresh(applicationContext)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val NAME = "ods_status_refresh"

        fun schedule(context: Context) {
            // Never let scheduling crash app launch (e.g. WorkManager init edge cases).
            runCatching {
                val request = PeriodicWorkRequestBuilder<StatusRefreshWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                    )
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    NAME, ExistingPeriodicWorkPolicy.KEEP, request,
                )
            }
        }
    }
}
