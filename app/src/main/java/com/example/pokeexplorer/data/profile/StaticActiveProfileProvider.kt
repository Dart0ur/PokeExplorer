package com.example.pokeexplorer.data.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Временная реализация: всегда профиль по умолчанию (id = 1).
 * Создан в миграции 1→2. На следующей итерации заменится на DataStore-версию.
 */
@Singleton
class StaticActiveProfileProvider @Inject constructor() : ActiveProfileProvider {
    override val activeProfileId: Flow<Long> = flowOf(DEFAULT_PROFILE_ID)

    companion object {
        const val DEFAULT_PROFILE_ID = 1L
    }
}