package com.vetstop.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vetstop.app.data.prefs.SettingsRepository
import com.vetstop.app.data.repo.PlaceSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background job that refreshes the location database from the Places API.
 * Scheduled weekly by [SyncScheduler]; also run on demand from Settings.
 */
@HiltWorker
class PlaceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val placeSyncRepository: PlaceSyncRepository,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val result = placeSyncRepository.syncAllAreas()
            val summary =
                "${result.areasSynced} area(s): +${result.added} new, " +
                    "-${result.removed} removed, ${result.totalActive} total"
            settingsRepository.recordSyncResult(System.currentTimeMillis(), summary)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "place_sync_weekly"
        const val ONE_TIME_WORK_NAME = "place_sync_now"
        private const val MAX_RETRIES = 3
    }
}
