package com.example.pokeexplorer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Скрытые покемоны для профиля. Не показываются в списке, если настройка
 * «показывать скрытых» выключена (DataStore).
 */
@Entity(
    tableName = "hidden_pokemon",
    primaryKeys = ["profileId", "pokemonId"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("profileId")],
)
data class HiddenPokemonEntity(
    val profileId: Long,
    val pokemonId: Int,
    val hiddenAt: Long = System.currentTimeMillis(),
)
