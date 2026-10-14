package com.example.pokeexplorer.integration

import com.example.pokeexplorer.data.favourites.FavouritesRepository
import com.example.pokeexplorer.data.model.PokemonListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Подмена хранилища избранного.
 *
 * Повторяет контракт настоящего Room-репозитория: toggle добавляет,
 * если элемента нет, и удаляет, если есть. Именно на этом контракте
 * ловится баг с дублями, поэтому фейк должен вести себя так же, как боевой код.
 */
class FakeFavouritesRepository : FavouritesRepository {

    private val state = MutableStateFlow<List<PokemonListItem>>(emptyList())

    override val favourites: Flow<List<PokemonListItem>> = state

    override val ids: Flow<Set<Int>> = state.map { list -> list.map { it.id }.toSet() }

    /** Сколько раз реально ходили в хранилище — для проверки, что UI не долбит БД зря. */
    var toggleCalls: Int = 0
        private set

    override suspend fun isFavourite(id: Int): Boolean = state.value.any { it.id == id }

    override suspend fun toggle(item: PokemonListItem) {
        toggleCalls++
        val current = state.value
        state.value = if (current.any { it.id == item.id }) {
            current.filterNot { it.id == item.id }
        } else {
            listOf(item) + current
        }
    }

    override suspend fun remove(id: Int) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun clear() {
        state.value = emptyList()
    }

    /** Текущее содержимое — удобно для быстрых проверок без Turbine. */
    val current: List<PokemonListItem> get() = state.value
}
