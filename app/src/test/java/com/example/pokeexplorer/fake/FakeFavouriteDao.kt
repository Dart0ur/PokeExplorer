package com.example.pokeexplorer.fake

import com.example.pokeexplorer.data.local.FavouriteDao
import com.example.pokeexplorer.data.local.FavouriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Подмена DAO без Room — для чистых юнит-тестов репозитория.
 * Повторяет поведение FavouriteDao версии 2: все данные разделены по profileId.
 */
class FakeFavouriteDao : FavouriteDao {

    private val rows = MutableStateFlow<List<FavouriteEntity>>(emptyList())

    /** Все insert'ы, включая перезаписи — по ним ловим дубли. */
    val inserted = mutableListOf<FavouriteEntity>()

    override fun observeAll(profileId: Long): Flow<List<FavouriteEntity>> =
        rows.map { list -> list.filter { it.profileId == profileId }.sortedByDescending { it.addedAt } }

    override fun observeIds(profileId: Long): Flow<List<Int>> =
        rows.map { list -> list.filter { it.profileId == profileId }.map { it.id } }

    override suspend fun exists(profileId: Long, id: Int): Boolean =
        rows.value.any { it.profileId == profileId && it.id == id }

    /** REPLACE по паре (profileId, id). */
    override suspend fun insert(entity: FavouriteEntity) {
        inserted += entity
        rows.value = rows.value.filterNot { it.profileId == entity.profileId && it.id == entity.id } + entity
    }

    override suspend fun deleteById(profileId: Long, id: Int) {
        rows.value = rows.value.filterNot { it.profileId == profileId && it.id == id }
    }

    override suspend fun clear(profileId: Long) {
        rows.value = rows.value.filterNot { it.profileId == profileId }
    }
}