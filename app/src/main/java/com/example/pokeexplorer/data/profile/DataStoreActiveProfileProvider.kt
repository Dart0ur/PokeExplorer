package com.example.pokeexplorer.data.profile

import com.example.pokeexplorer.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация ActiveProfileProvider через DataStore.
 * Заменяет StaticActiveProfileProvider из итерации 1.
 */
@Singleton
class DataStoreActiveProfileProvider @Inject constructor(
    private val preferences: UserPreferencesRepository,
) : ActiveProfileProvider {
    override val activeProfileId: Flow<Long> = preferences.activeProfileId
}