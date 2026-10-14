package com.example.pokeexplorer.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pokeexplorer.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Постановка фоновой синхронизации в очередь.
 *
 * Настройка «Только Wi-Fi» активного профиля превращается в ограничение
 * задачи: при Wi-Fi задача пойдёт только по сети без лимита,
 * иначе — по любой сети.
 */

/** Состояние ручной синхронизации для экрана настроек. */
enum class SyncStatus { IDLE, WAITING_FOR_NETWORK, RUNNING }
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesRepository,
) {

    /**
     * Гарантирует, что периодическая задача стоит в очереди.
     * Вызывается при старте приложения.
     */
    suspend fun ensurePeriodicScheduled() {
        val request = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
            repeatInterval = SYNC_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(currentConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            OfflineSyncWorker.PERIODIC_NAME,
            // UPDATE, а не KEEP: если пользователь сменил «Только Wi-Fi»,
            // ограничения уже стоящей задачи нужно обновить.
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Разовое обновление — для кнопки «Обновить сейчас» на экране настроек. */
    suspend fun runOnce() {
        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(currentConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            OfflineSyncWorker.MANUAL_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** Отменить фоновую синхронизацию целиком. */
    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(OfflineSyncWorker.PERIODIC_NAME)
    }

    /**
     * Следит за кнопкой «Обновить сейчас»: ждёт ли задача сети, идёт ли она.
     * Именно функция, а не свойство: WorkManager нельзя трогать,
     * пока Hilt ещё внедряет зависимости в Application.
     */
    fun observeManualSync(): Flow<SyncStatus> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(OfflineSyncWorker.MANUAL_NAME)
            .map { infos ->
                when {
                    infos.any { it.state == WorkInfo.State.RUNNING } -> SyncStatus.RUNNING
                    infos.any { it.state == WorkInfo.State.ENQUEUED } -> SyncStatus.WAITING_FOR_NETWORK
                    else -> SyncStatus.IDLE
                }
            }

    private suspend fun currentConstraints(): Constraints {
        val profileId = preferences.activeProfileId.first()
        val wifiOnly = preferences.getWifiOnly(profileId).first()

        return Constraints.Builder()
            .setRequiredNetworkType(
                if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .build()
    }

    companion object {
        const val SYNC_INTERVAL_HOURS = 12L
    }
}