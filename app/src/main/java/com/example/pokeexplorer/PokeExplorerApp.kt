package com.example.pokeexplorer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.pokeexplorer.data.preferences.UserPreferencesRepository
import com.example.pokeexplorer.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application-класс: точка входа Hilt и настройка WorkManager.
 *
 * Configuration.Provider подсовывает WorkManager фабрику Hilt, иначе
 * воркер с @HiltWorker не сможет получить зависимости в конструктор.
 */
@HiltAndroidApp
class PokeExplorerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var preferences: UserPreferencesRepository

    /** Живёт столько же, сколько процесс приложения. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        // «Только Wi-Fi» активного профиля → ограничение фоновой задачи.
        // Срабатывает при старте, при смене настройки и при смене профиля.
        appScope.launch {
            preferences.activeProfileId
                .flatMapLatest { profileId -> preferences.getWifiOnly(profileId) }
                .distinctUntilChanged()
                .collect { syncScheduler.ensurePeriodicScheduled() }
        }
    }
}