package com.example.pokeexplorer.fake

import com.example.pokeexplorer.data.hidden.HiddenPokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Скрытые покемоны в памяти. Тест может сам задавать набор и настройку. */
class FakeHiddenRepository : HiddenPokemonRepository {
    val ids = MutableStateFlow<Set<Int>>(emptySet())
    val show = MutableStateFlow(false)

    override val hiddenIds: Flow<Set<Int>> = ids
    override val showHidden: Flow<Boolean> = show

    override suspend fun toggle(pokemonId: Int) {
        ids.update { if (pokemonId in it) it - pokemonId else it + pokemonId }
    }
}