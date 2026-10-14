package com.example.pokeexplorer.data.history

import com.example.pokeexplorer.data.local.HistoryDao
import com.example.pokeexplorer.data.local.HistoryEntity
import com.example.pokeexplorer.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Запись истории для UI — без технического profileId. */
data class HistoryItem(
    val pokemonId: Int,
    val name: String,
    val imageUrl: String,
    val viewedAt: Long,
)

/**
 * История просмотров активного профиля.
 * При смене профиля flatMapLatest переключается на его записи.
 */
@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao,
    private val preferences: UserPreferencesRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val recent: Flow<List<HistoryItem>> = preferences.activeProfileId
        .flatMapLatest { profileId -> dao.observeByProfile(profileId) }
        .map { list -> list.map { it.toItem() } }

    /** Повторный просмотр обновляет время (REPLACE), лишнее срезается до 20. */
    suspend fun record(pokemonId: Int, name: String, imageUrl: String) {
        val profileId = preferences.activeProfileId.first()
        dao.insert(
            HistoryEntity(
                profileId = profileId,
                pokemonId = pokemonId,
                pokemonName = name,
                pokemonImageUrl = imageUrl,
                viewedAt = System.currentTimeMillis(),
            )
        )
        dao.trimToLimit(profileId)
    }

    suspend fun remove(pokemonId: Int) {
        dao.remove(preferences.activeProfileId.first(), pokemonId)
    }

    suspend fun clear() {
        dao.clearForProfile(preferences.activeProfileId.first())
    }
}

private fun HistoryEntity.toItem() = HistoryItem(
    pokemonId = pokemonId,
    name = pokemonName,
    imageUrl = pokemonImageUrl,
    viewedAt = viewedAt,
)