package com.example.pokeexplorer.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Индекс покемонов для списка: id, имя, ссылка. Грузится один раз
 * и кэшируется, чтобы поиск и фильтр работали офлайн.
 * Индекс по name ускоряет поиск; объявлен здесь и в MIGRATION_2_3 одинаково.
 */
@Entity(
    tableName = "pokemon_index",
    indices = [Index("name")],
)
data class PokemonIndexEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val imageUrl: String,
    val fetchedAt: Long = System.currentTimeMillis(),
)