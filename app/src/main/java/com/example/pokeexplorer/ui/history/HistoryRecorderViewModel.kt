package com.example.pokeexplorer.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeexplorer.data.history.HistoryRepository
import com.example.pokeexplorer.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Записывает открытие экрана деталей в историю активного профиля.
 *
 * Запись делается в init. ViewModel живёт столько же, сколько экран
 * деталей в стеке навигации, поэтому поворот экрана не создаёт
 * повторную запись. Повторное открытие того же покемона обновляет
 * время (REPLACE в HistoryDao), а не добавляет вторую строку.
 */
@HiltViewModel
class HistoryRecorderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    init {
        val id = savedStateHandle.get<String>(Routes.DETAIL_ARG_ID)?.toIntOrNull()
        if (id != null) {
            val name = savedStateHandle.get<String>(Routes.DETAIL_ARG_NAME)
                .orEmpty()
                .ifBlank { "#$id" }
            viewModelScope.launch {
                historyRepository.record(pokemonId = id, name = name, imageUrl = artworkUrl(id))
            }
        }
    }

    private fun artworkUrl(id: Int) =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
}