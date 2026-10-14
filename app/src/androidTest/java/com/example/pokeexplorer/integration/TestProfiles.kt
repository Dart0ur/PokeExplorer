package com.example.pokeexplorer.integration

import com.example.pokeexplorer.data.local.PokeDatabase
import com.example.pokeexplorer.data.profile.ActiveProfileProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Управляемый активный профиль для тестов: вместо DataStore —
 * MutableStateFlow, который тест переключает вручную.
 */
class TestActiveProfileProvider(initial: Long = 1L) : ActiveProfileProvider {
    private val state = MutableStateFlow(initial)
    override val activeProfileId: Flow<Long> = state

    fun switchTo(profileId: Long) {
        state.value = profileId
    }
}

/**
 * В in-memory базе нет callback'а с профилем по умолчанию,
 * поэтому профиль нужно создать явно — иначе FOREIGN KEY constraint failed.
 */
fun PokeDatabase.insertTestProfile(id: Long, name: String = "Тест") {
    openHelper.writableDatabase.execSQL(
        "INSERT OR IGNORE INTO profiles (id, name, createdAt) VALUES ($id, '$name', 0)"
    )
}