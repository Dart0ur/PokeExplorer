package com.example.pokeexplorer.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeexplorer.data.history.HistoryItem
import com.example.pokeexplorer.data.history.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val items: List<HistoryItem> = emptyList(),
)

sealed interface HistoryEvent {
    data class OnRemove(val pokemonId: Int) : HistoryEvent
    data object OnClearAll : HistoryEvent
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = repository.recent
        .map { HistoryUiState(isLoading = false, items = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onEvent(event: HistoryEvent) {
        viewModelScope.launch {
            when (event) {
                is HistoryEvent.OnRemove -> repository.remove(event.pokemonId)
                HistoryEvent.OnClearAll -> repository.clear()
            }
        }
    }
}