package com.example.pokeexplorer.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeexplorer.data.favourites.FavouritesRepository
import com.example.pokeexplorer.data.model.PokemonListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Экран «Избранное» — отдельный route.
 * Сети здесь нет, поэтому состояние сводится к «список / пусто».
 * Источник — Room: при изменении таблицы Flow сам пришлёт новый список.
 */
data class FavouritesUiState(
    val items: List<PokemonListItem> = emptyList(),
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

sealed interface FavouritesEvent {
    data class OnRemove(val id: Int) : FavouritesEvent
    data object OnClearAll : FavouritesEvent
}

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val favouritesRepository: FavouritesRepository,
) : ViewModel() {

    /**
     * Состояние собираем из Room-Flow. WhileSubscribed — пока экран на переднем
     * плане, подписка жива; 5 секунд держим после ухода (поворот экрана).
     */
    val uiState: StateFlow<FavouritesUiState> = favouritesRepository.favourites
        .map { FavouritesUiState(items = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavouritesUiState(),
        )

    fun onEvent(event: FavouritesEvent) {
        viewModelScope.launch {
            when (event) {
                is FavouritesEvent.OnRemove -> favouritesRepository.remove(event.id)
                FavouritesEvent.OnClearAll -> favouritesRepository.clear()
            }
        }
    }
}
