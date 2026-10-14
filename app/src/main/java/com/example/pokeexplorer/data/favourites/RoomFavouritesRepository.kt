package com.example.pokeexplorer.data.favourites

import com.example.pokeexplorer.data.local.FavouriteDao
import com.example.pokeexplorer.data.local.FavouriteEntity
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.data.profile.ActiveProfileProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация поверх Room. Избранное разделено по профилям:
 * при смене профиля flatMapLatest переподписывается на другой запрос,
 * и UI получает данные нового профиля автоматически.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RoomFavouritesRepository @Inject constructor(
    private val dao: FavouriteDao,
    private val activeProfile: ActiveProfileProvider,
) : FavouritesRepository {

    override val favourites: Flow<List<PokemonListItem>> =
        activeProfile.activeProfileId
            .flatMapLatest { profileId -> dao.observeAll(profileId) }
            .map { entities -> entities.map { it.toDomain() } }

    override val ids: Flow<Set<Int>> =
        activeProfile.activeProfileId
            .flatMapLatest { profileId -> dao.observeIds(profileId) }
            .map { it.toSet() }

    override suspend fun isFavourite(id: Int): Boolean =
        dao.exists(activeProfile.activeProfileId.first(), id)

    override suspend fun toggle(item: PokemonListItem) {
        val profileId = activeProfile.activeProfileId.first()
        if (dao.exists(profileId, item.id)) {
            dao.deleteById(profileId, item.id)
        } else {
            dao.insert(item.toEntity(profileId))
        }
    }

    override suspend fun remove(id: Int) =
        dao.deleteById(activeProfile.activeProfileId.first(), id)

    override suspend fun clear() =
        dao.clear(activeProfile.activeProfileId.first())

    // --- маппинг между слоями: Entity (БД) <-> domain-модель (UI) ---

    private fun FavouriteEntity.toDomain() = PokemonListItem(
        id = id,
        name = name,
        imageUrl = imageUrl,
    )

    private fun PokemonListItem.toEntity(profileId: Long) = FavouriteEntity(
        profileId = profileId,
        id = id,
        name = name,
        imageUrl = imageUrl,
        addedAt = System.currentTimeMillis(),
    )
}