package com.example.pokeexplorer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeexplorer.data.preferences.UserPreferencesRepository
import com.example.pokeexplorer.data.sync.SyncScheduler
import com.example.pokeexplorer.data.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoaded: Boolean = false,
    val ttlHours: Int = 24,
    val wifiOnly: Boolean = false,
    val showHidden: Boolean = false,
    val lastSyncAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
)

sealed interface SettingsEvent {
    data class OnTtlSelected(val hours: Int) : SettingsEvent
    data class OnWifiOnlyChanged(val enabled: Boolean) : SettingsEvent
    data class OnShowHiddenChanged(val enabled: Boolean) : SettingsEvent
    data object OnSyncNow : SettingsEvent
}

/**
 * Настройки активного профиля. При смене профиля flatMapLatest
 * переключается на его ключи — экран сразу показывает его значения.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val profileSettings = preferences.activeProfileId.flatMapLatest { id ->
        combine(
            preferences.getTtlHours(id),
            preferences.getWifiOnly(id),
            preferences.getShowHidden(id),
        ) { ttl, wifiOnly, showHidden ->
            SettingsUiState(
                isLoaded = true,
                ttlHours = ttl,
                wifiOnly = wifiOnly,
                showHidden = showHidden,
            )
        }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        profileSettings,
        preferences.lastSyncAt,
        syncScheduler.observeManualSync(),
    ) { settings, lastSyncAt, status ->
        settings.copy(lastSyncAt = lastSyncAt, syncStatus = status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            val profileId = preferences.activeProfileId.first()
            when (event) {
                is SettingsEvent.OnTtlSelected -> preferences.setTtlHours(profileId, event.hours)
                // Перепланирование задачи сделает PokeExplorerApp — он следит за этой настройкой.
                is SettingsEvent.OnWifiOnlyChanged -> preferences.setWifiOnly(profileId, event.enabled)
                is SettingsEvent.OnShowHiddenChanged -> preferences.setShowHidden(profileId, event.enabled)
                SettingsEvent.OnSyncNow -> syncScheduler.runOnce()
            }
        }
    }

    companion object {
        /** 0 — «всегда обновлять» (CachePolicy считает такие данные устаревшими). */
        val TTL_OPTIONS = listOf(0, 1, 6, 24, 72)
    }
}