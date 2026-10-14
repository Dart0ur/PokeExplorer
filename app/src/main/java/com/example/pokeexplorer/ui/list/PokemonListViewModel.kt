package com.example.pokeexplorer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeexplorer.core.ErrorMapper
import com.example.pokeexplorer.core.UiState
import com.example.pokeexplorer.data.PokemonRepository
import com.example.pokeexplorer.data.favourites.FavouritesRepository
import com.example.pokeexplorer.data.hidden.HiddenPokemonRepository
import com.example.pokeexplorer.data.model.PokemonListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FilterMode { ALL, FAVOURITES_ONLY }

data class PokemonListContent(
    val items: List<PokemonListItem>,
    val totalCount: Int,
    val hasMore: Boolean,
    val isSearchMode: Boolean,
)

data class PokemonListUiState(
    val query: String = "",
    val filterMode: FilterMode = FilterMode.ALL,
    val content: UiState<PokemonListContent> = UiState.Loading,
    val favouriteIds: Set<Int> = emptySet(),
    val hiddenIds: Set<Int> = emptySet(),
)

sealed interface PokemonListEvent {
    data class OnQueryChange(val query: String) : PokemonListEvent
    data object OnClearQuery : PokemonListEvent
    data class OnFilterChange(val mode: FilterMode) : PokemonListEvent
    data object OnRefresh : PokemonListEvent
    data object OnRetry : PokemonListEvent
    data class OnToggleFavourite(val item: PokemonListItem) : PokemonListEvent
    data class OnToggleHidden(val item: PokemonListItem) : PokemonListEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PokemonListViewModel @Inject constructor(
    private val repository: PokemonRepository,
    private val favouritesRepository: FavouritesRepository,
    private val hiddenRepository: HiddenPokemonRepository = HiddenPokemonRepository.None,
) : ViewModel() {

    private val _queryFlow = MutableStateFlow("")
    private val _filterFlow = MutableStateFlow(FilterMode.ALL)

    /** Счётчик нажатий «Обновить»/«Повторить»: каждое новое значение — новая загрузка. */
    private val _refreshCounter = MutableStateFlow(0)

    /** Скрытые id + настройка «Показывать скрытых» активного профиля. */
    private val hiddenState = combine(hiddenRepository.hiddenIds, hiddenRepository.showHidden) { ids, show ->
        ids to show
    }

    val uiState: StateFlow<PokemonListUiState> =
        combine(_queryFlow, _filterFlow, _refreshCounter) { q, f, r -> Triple(q, f, r) }
            .debounce { (query, _, _) -> if (query.isNotBlank()) DEBOUNCE_MS else 0L }
            // Сравниваем все три значения, иначе нажатие «Обновить» отбрасывается.
            .distinctUntilChanged()
            .flatMapLatest { (query, filter, _) -> loadData(query.trim(), filter) }
            .combine(favouritesRepository.ids) { content, favIds -> content to favIds }
            // Скрытие — фильтр поверх загруженных данных, без повторного запроса в сеть.
            .combine(hiddenState) { (content, favIds), (hiddenIds, showHidden) ->
                PokemonListUiState(
                    query = _queryFlow.value,
                    filterMode = _filterFlow.value,
                    content = content.withoutHidden(hiddenIds, showHidden),
                    favouriteIds = favIds,
                    hiddenIds = hiddenIds,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PokemonListUiState())

    fun onEvent(event: PokemonListEvent) {
        when (event) {
            is PokemonListEvent.OnQueryChange -> _queryFlow.value = event.query
            PokemonListEvent.OnClearQuery -> _queryFlow.value = ""
            is PokemonListEvent.OnFilterChange -> _filterFlow.value = event.mode
            PokemonListEvent.OnRefresh, PokemonListEvent.OnRetry -> _refreshCounter.update { it + 1 }
            is PokemonListEvent.OnToggleFavourite ->
                viewModelScope.launch { favouritesRepository.toggle(event.item) }
            is PokemonListEvent.OnToggleHidden ->
                viewModelScope.launch { hiddenRepository.toggle(event.item.id) }
        }
    }

    /**
     * Одна загрузка. Ошибку ловим здесь, а не в конце общей цепочки:
     * так падает только эта загрузка, а uiState продолжает слушать
     * запрос, фильтр и «Повторить».
     */
    private fun loadData(query: String, filter: FilterMode): Flow<UiState<PokemonListContent>> = flow {
        emit(UiState.Loading)
        when {
            // Избранные берём прямо из Room: работает без сети, а снятие
            // сердечка сразу убирает покемона из списка. Поиск тоже учитывается.
            filter == FilterMode.FAVOURITES_ONLY -> emitAll(
                favouritesRepository.favourites.map { favs ->
                    val filtered = if (query.isBlank()) favs
                    else favs.filter { it.name.contains(query, ignoreCase = true) }
                    if (filtered.isEmpty()) UiState.Empty
                    else UiState.Success(PokemonListContent(filtered, filtered.size, false, true))
                }
            )
            query.isNotBlank() -> {
                val index = repository.getPokemonIndex().getOrThrow()
                val matches = index.filter { it.name.contains(query, ignoreCase = true) }
                emit(
                    if (matches.isEmpty()) UiState.Empty
                    else UiState.Success(
                        PokemonListContent(matches.take(PAGE_SIZE), matches.size, matches.size > PAGE_SIZE, true)
                    )
                )
            }
            else -> {
                val page = repository.getPokemonPage(0, PAGE_SIZE).getOrThrow()
                emit(
                    if (page.items.isEmpty()) UiState.Empty
                    else UiState.Success(
                        PokemonListContent(page.items, page.totalCount, page.hasMore, false)
                    )
                )
            }
        }
    }.catch { emit(UiState.Error(ErrorMapper.messageFor(it))) }

    companion object {
        const val DEBOUNCE_MS = 400L
        const val PAGE_SIZE = 30
    }
}

/** Убирает скрытых, если «Показывать скрытых» выключено. */
private fun UiState<PokemonListContent>.withoutHidden(
    hiddenIds: Set<Int>,
    showHidden: Boolean,
): UiState<PokemonListContent> {
    if (showHidden || hiddenIds.isEmpty() || this !is UiState.Success) return this
    val visible = data.items.filterNot { it.id in hiddenIds }
    return if (visible.isEmpty()) UiState.Empty else UiState.Success(data.copy(items = visible))
}