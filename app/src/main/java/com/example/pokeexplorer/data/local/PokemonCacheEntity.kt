package com.example.pokeexplorer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Кэш деталей покемонов с TTL. Используется для offline-first:
 * свежие детали (<24 ч) показываются без сети, устаревшие — с пометкой
 * и фоновым обновлением.
 */
@Entity(
    tableName = "pokemon_cache",
    primaryKeys = ["pokemonId"],
)
data class PokemonCacheEntity(
    val pokemonId: Int,
    val name: String,
    val imageUrl: String,
    val heightDecimetres: Int,
    val weightHectograms: Int,
    val baseExperience: Int?,
    val typesJson: String, // JSON-массив ["electric", "flying"]
    val statsJson: String, // JSON-массив [{"name":"hp","value":35}, ...]
    val abilitiesJson: String,
    val description: String?,
    val genus: String?,
    val captureRate: Int?,
    val habitat: String?,
    val isLegendary: Boolean,
    val isMythical: Boolean,
    val fetchedAt: Long = System.currentTimeMillis(),
)
