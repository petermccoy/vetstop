package com.vetstop.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Idempotent: keeps the existing schedule if one is already enqueued. */
    fun scheduleWeeklySync() {
        val request = PeriodicWorkRequestBuilder<PlaceSyncWorker>(7, TimeUnit.DAYS)
            .setConstraints(networkConstraint)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PlaceSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelWeeklySync() {
        workManager.cancelUniqueWork(PlaceSyncWorker.PERIODIC_WORK_NAME)
    }

    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<PlaceSyncWorker>()
            .setConstraints(networkConstraint)
            .build()
        workManager.enqueueUniqueWork(
            PlaceSyncWorker.ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun observeSyncNow(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(PlaceSyncWorker.ONE_TIME_WORK_NAME)

    fun observeWeeklySync(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(PlaceSyncWorker.PERIODIC_WORK_NAME)
}
