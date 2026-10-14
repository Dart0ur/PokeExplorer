package com.example.pokeexplorer.fake

import com.example.pokeexplorer.data.profile.ActiveProfileProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Активный профиль для юнит-тестов: переключается вручную. */
class FakeActiveProfileProvider(initial: Long = 1L) : ActiveProfileProvider {
    private val state = MutableStateFlow(initial)
    override val activeProfileId: Flow<Long> = state

    fun switchTo(profileId: Long) {
        state.value = profileId
    }
}