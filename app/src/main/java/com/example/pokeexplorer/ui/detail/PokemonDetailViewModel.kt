package com.example.pokeexplorer.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeexplorer.core.ErrorMapper
import com.example.pokeexplorer.core.UiState
import com.example.pokeexplorer.core.resolveIntArgument
import com.example.pokeexplorer.data.PokemonRepository
import com.example.pokeexplorer.data.favourites.FavouritesRepository
import com.example.pokeexplorer.data.mapper.artworkUrl
import com.example.pokeexplorer.data.model.PokemonDetail
import com.example.pokeexplorer.data.model.PokemonListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PokemonDetailUiState(
    val id: Int = 0,
    val name: String = "",
    val imageUrl: String = "",
    val detail: UiState<PokemonDetail> = UiState.Loading,
    val isFavourite: Boolean = false,
)

sealed interface PokemonDetailEvent {
    data object OnRetry : PokemonDetailEvent
    data object OnToggleFavourite : PokemonDetailEvent
}

/**
 * Экран деталей. Аргументы route читаются из SavedStateHandle — Hilt умеет его
 * внедрять, и для ViewModel это штатный способ получить id/name.
 *
 * Разбор id остался через resolveIntArgument: NavHost объявляет аргумент как
 * StringType, но читать его нужно безопасно (см. core/NavigationArgs.kt).
 */
@HiltViewModel
class PokemonDetailViewModel @Inject constructor(
    private val repository: PokemonRepository,
    private val favouritesRepository: FavouritesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pokemonId: Int = resolveIntArgument(
        readInt = { savedStateHandle.get<Int>(ARG_ID) },
        readString = { savedStateHandle.get<String>(ARG_ID) },
    )

    // Имя приходит из списка query-параметром route — заголовок осмысленный
    // сразу, пока детали ещё грузятся.
    private var currentItem: PokemonListItem = PokemonListItem(
        id = pokemonId,
        name = savedStateHandle.get<String>(ARG_NAME).orEmpty(),
        imageUrl = artworkUrl(pokemonId),
    )

    var uiState: PokemonDetailUiState by mutableStateOf(
        PokemonDetailUiState(
            id = pokemonId,
            name = currentItem.name,
            imageUrl = currentItem.imageUrl,
            detail = UiState.Loading,
            isFavourite = false,
        )
    )
        private set

    init {
        // Избранное хранится в Room, поэтому проверка асинхронная.
        viewModelScope.launch {
            if (pokemonId > 0) {
                uiState = uiState.copy(
                    isFavourite = favouritesRepository.isFavourite(pokemonId),
                )
            }
        }
        loadDetail()
    }

    fun onEvent(event: PokemonDetailEvent) {
        when (event) {
            PokemonDetailEvent.OnRetry -> loadDetail()
            PokemonDetailEvent.OnToggleFavourite -> toggleFavourite()
        }
    }

    private fun loadDetail() {
        if (pokemonId <= 0) {
            uiState = uiState.copy(
                detail = UiState.Error("Некорректный id покемона ($pokemonId)"),
            )
            return
        }

        uiState = uiState.copy(detail = UiState.Loading)

        viewModelScope.launch {
            repository.getPokemonDetail(pokemonId)
                .onSuccess { detail ->
                    // Пришло настоящее имя — обновляем и его, и картинку.
                    currentItem = currentItem.copy(name = detail.name, imageUrl = detail.imageUrl)
                    uiState = uiState.copy(
                        name = detail.name,
                        imageUrl = detail.imageUrl,
                        detail = UiState.Success(detail),
                    )
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(detail = UiState.Error(ErrorMapper.messageFor(throwable)))
                }
        }
    }

    private fun toggleFavourite() {
        viewModelScope.launch {
            favouritesRepository.toggle(currentItem)
            uiState = uiState.copy(
                isFavourite = favouritesRepository.isFavourite(currentItem.id),
            )
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_NAME = "name"
    }
}
