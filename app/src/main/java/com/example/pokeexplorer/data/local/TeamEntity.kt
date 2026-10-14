package com.example.pokeexplorer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Команда покемонов. У каждого профиля может быть несколько команд,
 * в каждой команде до 6 покемонов (проверка в Repository).
 */
@Entity(
    tableName = "teams",
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
data class TeamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Член команды (junction table для связи многие-ко-многим).
 * У каждой связи есть поля: позиция в команде (1-6) и заметка.
 */
@Entity(
    tableName = "team_members",
    primaryKeys = ["teamId", "pokemonId"],
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("teamId"), Index("pokemonId")],
)
data class TeamMemberEntity(
    val teamId: Long,
    val pokemonId: Int,
    val pokemonName: String,
    val pokemonImageUrl: String,
    val position: Int, // 1-6
    val note: String = "",
    val addedAt: Long = System.currentTimeMillis(),
)
