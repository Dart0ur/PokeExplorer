package com.example.pokeexplorer.ui.teams

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeexplorer.data.teams.TeamMember
import com.example.pokeexplorer.data.teams.TeamRepository
import com.example.pokeexplorer.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamDetailUiState(
    val isLoading: Boolean = true,
    val teamExists: Boolean = true,
    val teamName: String = "",
    val members: List<TeamMember> = emptyList(),
)

sealed interface TeamDetailEvent {
    data class OnRemove(val pokemonId: Int) : TeamDetailEvent
    data class OnEditNote(val pokemonId: Int, val note: String) : TeamDetailEvent
}

/**
 * Одна команда: название и состав. teamId приходит из маршрута team/{teamId}.
 * Если команду удалили или она принадлежит другому профилю, teamExists = false
 * и экран закрывается сам.
 */
@HiltViewModel
class TeamDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TeamRepository,
) : ViewModel() {

    private val teamId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.TEAM_ARG_ID)) {
        "TeamDetailViewModel требует аргумент ${Routes.TEAM_ARG_ID}"
    }

    val uiState: StateFlow<TeamDetailUiState> = combine(
        repository.observeTeam(teamId),
        repository.observeMembers(teamId),
    ) { team, members ->
        TeamDetailUiState(
            isLoading = false,
            teamExists = team != null,
            teamName = team?.name.orEmpty(),
            members = members,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamDetailUiState())

    fun onEvent(event: TeamDetailEvent) {
        viewModelScope.launch {
            when (event) {
                is TeamDetailEvent.OnRemove -> repository.removeMember(teamId, event.pokemonId)
                is TeamDetailEvent.OnEditNote -> repository.updateNote(teamId, event.pokemonId, event.note)
            }
        }
    }
}