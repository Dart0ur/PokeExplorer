package com.example.pokeexplorer.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранилище настроек. Глобально живут только activeProfileId и время
 * последней синхронизации, остальные настройки хранятся с ключами
 * по профилю (ttl_hours_2). Это выполняет требование ТЗ:
 * "Настройки должны быть разделены по профилям."
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val Context.dataStore by preferencesDataStore(name = "user_preferences")

    companion object {
        private val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
        private val LAST_SYNC_AT = longPreferencesKey("last_sync_at")

        private const val DEFAULT_PROFILE_ID = 1L
        private const val DEFAULT_TTL_HOURS = 24
        private const val DEFAULT_WIFI_ONLY = false
        private const val DEFAULT_SHOW_HIDDEN = false

        // Для настроек по профилю ключи строятся динамически:
        // ttl_hours_2, wifi_only_2, show_hidden_2
        private fun ttlKey(profileId: Long) = intPreferencesKey("ttl_hours_$profileId")
        private fun wifiOnlyKey(profileId: Long) = booleanPreferencesKey("wifi_only_$profileId")
        private fun showHiddenKey(profileId: Long) = booleanPreferencesKey("show_hidden_$profileId")
    }

    // --- Активный профиль (глобальная настройка) ---

    val activeProfileId: Flow<Long> = context.dataStore.data
        .map { it[ACTIVE_PROFILE_ID] ?: DEFAULT_PROFILE_ID }

    suspend fun setActiveProfile(profileId: Long) {
        context.dataStore.edit { it[ACTIVE_PROFILE_ID] = profileId }
    }

    // --- Настройки по профилю ---

    fun getTtlHours(profileId: Long): Flow<Int> = context.dataStore.data
        .map { it[ttlKey(profileId)] ?: DEFAULT_TTL_HOURS }

    suspend fun setTtlHours(profileId: Long, hours: Int) {
        context.dataStore.edit { it[ttlKey(profileId)] = hours }
    }

    fun getWifiOnly(profileId: Long): Flow<Boolean> = context.dataStore.data
        .map { it[wifiOnlyKey(profileId)] ?: DEFAULT_WIFI_ONLY }

    suspend fun setWifiOnly(profileId: Long, enabled: Boolean) {
        context.dataStore.edit { it[wifiOnlyKey(profileId)] = enabled }
    }

    fun getShowHidden(profileId: Long): Flow<Boolean> = context.dataStore.data
        .map { it[showHiddenKey(profileId)] ?: DEFAULT_SHOW_HIDDEN }

    suspend fun setShowHidden(profileId: Long, enabled: Boolean) {
        context.dataStore.edit { it[showHiddenKey(profileId)] = enabled }
    }

    // --- Состояние синхронизации (общее для всех профилей) ---

    /** Время последней удачной фоновой синхронизации. null — ещё ни разу. */
    val lastSyncAt: Flow<Long?> = context.dataStore.data
        .map { it[LAST_SYNC_AT] }

    suspend fun setLastSyncAt(timestamp: Long) {
        context.dataStore.edit { it[LAST_SYNC_AT] = timestamp }
    }
}