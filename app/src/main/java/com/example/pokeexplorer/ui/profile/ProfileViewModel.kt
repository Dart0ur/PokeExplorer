package com.example.pokeexplorer.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeexplorer.data.local.ProfileEntity
import com.example.pokeexplorer.data.preferences.UserPreferencesRepository
import com.example.pokeexplorer.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel экрана профилей. Показывает список, создаёт, переключает, удаляет.
 * activeProfileId из DataStore подсвечивает текущий профиль в списке.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.profiles,
                preferences.activeProfileId,
            ) { profiles, activeId ->
                ProfileUiState(
                    profiles = profiles,
                    activeProfileId = activeId,
                )
            }.collect { _uiState.value = it }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.CreateProfile -> createProfile(event.name)
            is ProfileEvent.SwitchProfile -> switchProfile(event.profileId)
            is ProfileEvent.DeleteProfile -> deleteProfile(event.profile)
        }
    }

    private fun createProfile(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = repository.create(name.trim())
            // Автоматически переключаемся на новый профиль
            preferences.setActiveProfile(newId)
        }
    }

    private fun switchProfile(profileId: Long) {
        viewModelScope.launch {
            preferences.setActiveProfile(profileId)
        }
    }

    private fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            val count = repository.count()
            // Не даём удалить последний профиль
            if (count <= 1) return@launch

            val currentActive = preferences.activeProfileId.first()
            repository.delete(profile)

            // Если удалили активный, переключаемся на первый оставшийся
            if (profile.id == currentActive) {
                val remaining = repository.profiles.first().firstOrNull()
                remaining?.let { preferences.setActiveProfile(it.id) }
            }
        }
    }
}

data class ProfileUiState(
    val profiles: List<ProfileEntity> = emptyList(),
    val activeProfileId: Long = 1,
)

sealed interface ProfileEvent {
    data class CreateProfile(val name: String) : ProfileEvent
    data class SwitchProfile(val profileId: Long) : ProfileEvent
    data class DeleteProfile(val profile: ProfileEntity) : ProfileEvent
}