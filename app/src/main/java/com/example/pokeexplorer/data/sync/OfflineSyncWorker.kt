package com.example.pokeexplorer.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pokeexplorer.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Воркер фоновой синхронизации. Сам он ничего не делает —
 * вся работа в OfflineSyncEngine, а здесь только решение о результате.
 *
 * dependencies передаёт Hilt: для этого класс помечен @HiltWorker,
 * а параметры конструктора — @AssistedInject.
 */
@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val engine: OfflineSyncEngine,
    private val preferences: UserPreferencesRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val report = engine.run()

        // Совсем ничего не получилось — почти наверняка нет сети.
        // Просим WorkManager повторить с нарастающей паузой.
        if (report.isTotalFailure) {
            return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }

        preferences.setLastSyncAt(System.currentTimeMillis())
        return Result.success()
    }

    companion object {
        const val PERIODIC_NAME = "offline_sync_periodic"
        const val MANUAL_NAME = "offline_sync_manual"
        private const val MAX_ATTEMPTS = 3
    }
}