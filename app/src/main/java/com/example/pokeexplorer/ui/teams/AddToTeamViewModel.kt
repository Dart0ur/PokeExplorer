package com.example.pokeexplorer.ui.teams

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeexplorer.data.teams.AddToTeamResult
import com.example.pokeexplorer.data.teams.Team
import com.example.pokeexplorer.data.teams.TeamRepository
import com.example.pokeexplorer.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Добавление открытого покемона в команду. Живёт на экране деталей.
 * Результат (успех, команда заполнена, уже в команде) выдаётся текстом для Snackbar.
 */
@HiltViewModel
class AddToTeamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TeamRepository,
) : ViewModel() {

    private val pokemonId: Int? =
        savedStateHandle.get<String>(Routes.DETAIL_ARG_ID)?.toIntOrNull()

    private val pokemonName: String =
        savedStateHandle.get<String>(Routes.DETAIL_ARG_NAME).orEmpty()
            .ifBlank { pokemonId?.let { "#$it" }.orEmpty() }

    val teams: StateFlow<List<Team>> = repository.teams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun addTo(teamId: Long) {
        viewModelScope.launch { add(teamId) }
    }

    fun createTeamAndAdd(name: String) {
        viewModelScope.launch {
            val teamId = repository.createTeam(name)
            add(teamId, teamName = name.trim())
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    private suspend fun add(teamId: Long, teamName: String? = null) {
        val id = pokemonId ?: return
        val name = teamName ?: teams.value.firstOrNull { it.id == teamId }?.name ?: "команду"
        val displayName = pokemonName.replaceFirstChar { it.uppercase() }

        val result = repository.addMember(
            teamId = teamId,
            pokemonId = id,
            pokemonName = pokemonName,
            pokemonImageUrl = artworkUrl(id),
        )
        _message.value = when (result) {
            AddToTeamResult.Success -> "$displayName добавлен в «$name»"
            AddToTeamResult.AlreadyInTeam -> "$displayName уже в «$name»"
            AddToTeamResult.TeamFull ->
                "В «$name» уже ${TeamRepository.MAX_TEAM_SIZE} покемонов — это максимум"
        }
    }

    private fun artworkUrl(id: Int) =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
}