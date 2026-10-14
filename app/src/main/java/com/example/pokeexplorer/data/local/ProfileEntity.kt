package com.example.pokeexplorer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Профиль пользователя. Приложение поддерживает несколько локальных профилей
 * без серверной авторизации. Каждый профиль хранит свои команды, историю,
 * скрытых покемонов и избранное.
 *
 * Профиль по умолчанию (id=1, name="Тренер") создаётся при миграции БД.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)
