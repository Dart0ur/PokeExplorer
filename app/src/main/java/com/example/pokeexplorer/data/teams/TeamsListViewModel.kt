package com.example.pokeexplorer.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeexplorer.data.teams.Team
import com.example.pokeexplorer.data.teams.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamsListUiState(
    val isLoading: Boolean = true,
    val teams: List<Team> = emptyList(),
)

sealed interface TeamsListEvent {
    data class OnCreate(val name: String) : TeamsListEvent
    data class OnRename(val teamId: Long, val newName: String) : TeamsListEvent
    data class OnDelete(val teamId: Long) : TeamsListEvent
}

@HiltViewModel
class TeamsListViewModel @Inject constructor(
    private val repository: TeamRepository,
) : ViewModel() {

    val uiState: StateFlow<TeamsListUiState> = repository.teams
        .map { TeamsListUiState(isLoading = false, teams = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamsListUiState())

    fun onEvent(event: TeamsListEvent) {
        viewModelScope.launch {
            when (event) {
                is TeamsListEvent.OnCreate -> repository.createTeam(event.name)
                is TeamsListEvent.OnRename -> repository.renameTeam(event.teamId, event.newName)
                is TeamsListEvent.OnDelete -> repository.deleteTeam(event.teamId)
            }
        }
    }
}