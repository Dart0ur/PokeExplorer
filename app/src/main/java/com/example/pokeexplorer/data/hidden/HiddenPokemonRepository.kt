package com.example.pokeexplorer.data.hidden

import com.example.pokeexplorer.data.local.HiddenPokemonDao
import com.example.pokeexplorer.data.local.HiddenPokemonEntity
import com.example.pokeexplorer.data.preferences.UserPreferencesRepository
import com.example.pokeexplorer.data.profile.ActiveProfileProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Скрытые покемоны активного профиля и настройка «Показывать скрытых».
 * Интерфейс — чтобы ViewModel можно было тестировать без Room (см. None).
 */
interface HiddenPokemonRepository {
    val hiddenIds: Flow<Set<Int>>
    val showHidden: Flow<Boolean>
    suspend fun toggle(pokemonId: Int)

    /** Заглушка для тестов: ничего не скрыто. */
    object None : HiddenPokemonRepository {
        override val hiddenIds: Flow<Set<Int>> = flowOf(emptySet())
        override val showHidden: Flow<Boolean> = flowOf(false)
        override suspend fun toggle(pokemonId: Int) = Unit
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RoomHiddenPokemonRepository @Inject constructor(
    private val dao: HiddenPokemonDao,
    private val activeProfile: ActiveProfileProvider,
    private val preferences: UserPreferencesRepository,
) : HiddenPokemonRepository {

    override val hiddenIds: Flow<Set<Int>> = activeProfile.activeProfileId
        .flatMapLatest { profileId -> dao.observeIds(profileId) }
        .map { it.toSet() }

    override val showHidden: Flow<Boolean> = activeProfile.activeProfileId
        .flatMapLatest { profileId -> preferences.getShowHidden(profileId) }

    override suspend fun toggle(pokemonId: Int) {
        val profileId = activeProfile.activeProfileId.first()
        if (dao.isHidden(profileId, pokemonId)) {
            dao.remove(profileId, pokemonId)
        } else {
            dao.insert(
                HiddenPokemonEntity(
                    profileId = profileId,
                    pokemonId = pokemonId,
                    hiddenAt = System.currentTimeMillis(),
                )
            )
        }
    }
}
