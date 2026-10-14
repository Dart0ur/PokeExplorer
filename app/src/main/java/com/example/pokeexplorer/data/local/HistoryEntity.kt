package com.example.pokeexplorer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * История просмотров покемонов. Каждый профиль хранит до 20 последних.
 * Используется для экрана «Недавние» и фильтра «скрыть просмотренные».
 */
@Entity(
    tableName = "history",
    primaryKeys = ["profileId", "pokemonId"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("profileId"), Index("viewedAt")],
)
data class HistoryEntity(
    val profileId: Long,
    val pokemonId: Int,
    val pokemonName: String,
    val pokemonImageUrl: String,
    val viewedAt: Long = System.currentTimeMillis(),
)
