package com.example.pokeexplorer.data.teams

import com.example.pokeexplorer.data.local.TeamDao
import com.example.pokeexplorer.data.local.TeamEntity
import com.example.pokeexplorer.data.local.TeamMemberEntity
import com.example.pokeexplorer.data.profile.ActiveProfileProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Команда для UI без profileId. */
data class Team(
    val id: Long,
    val name: String,
    val memberCount: Int,
    val createdAt: Long,
)

/** Участник команды для UI. */
data class TeamMember(
    val pokemonId: Int,
    val name: String,
    val imageUrl: String,
    val position: Int,
    val note: String,
    val addedAt: Long,
)

/** Результат добавления покемона в команду. */
sealed interface AddToTeamResult {
    data object Success : AddToTeamResult
    data object TeamFull : AddToTeamResult
    data object AlreadyInTeam : AddToTeamResult
}

/**
 * Команды активного профиля. При смене профиля список переключается сам.
 * Лимит в 6 участников проверяется здесь, а не в SQL, чтобы его можно было протестировать.
 */
@Singleton
class TeamRepository @Inject constructor(
    private val dao: TeamDao,
    private val activeProfile: ActiveProfileProvider,
) {

    /** Проверка лимита и вставка должны идти одним шагом, иначе двойное нажатие даст 7 участников. */
    private val addMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    val teams: Flow<List<Team>> = activeProfile.activeProfileId
        .flatMapLatest { profileId ->
            combine(
                dao.observeByProfile(profileId),
                dao.observeMemberCounts(profileId),
            ) { teams, counts ->
                val countByTeam = counts.associate { it.teamId to it.memberCount }
                teams.map { it.toTeam(memberCount = countByTeam[it.id] ?: 0) }
            }
        }

    /** null — команда удалена или принадлежит другому профилю. */
    fun observeTeam(teamId: Long): Flow<Team?> =
        teams.map { list -> list.firstOrNull { it.id == teamId } }

    fun observeMembers(teamId: Long): Flow<List<TeamMember>> =
        dao.observeMembers(teamId).map { list -> list.map { it.toMember() } }

    suspend fun createTeam(name: String): Long {
        val profileId = activeProfile.activeProfileId.first()
        return dao.insertTeam(TeamEntity(profileId = profileId, name = name.trim()))
    }

    suspend fun renameTeam(teamId: Long, newName: String) {
        dao.getById(teamId)?.let { dao.updateTeam(it.copy(name = newName.trim())) }
    }

    /** Участники удаляются вместе с командой (ForeignKey.CASCADE). */
    suspend fun deleteTeam(teamId: Long) {
        dao.getById(teamId)?.let { dao.deleteTeam(it) }
    }

    /** Добавляет покемона на следующую свободную позицию (1..6). */
    suspend fun addMember(
        teamId: Long,
        pokemonId: Int,
        pokemonName: String,
        pokemonImageUrl: String,
        note: String = "",
    ): AddToTeamResult = addMutex.withLock {
        if (dao.isMember(teamId, pokemonId)) return@withLock AddToTeamResult.AlreadyInTeam
        val count = dao.getMemberCount(teamId)
        if (count >= MAX_TEAM_SIZE) return@withLock AddToTeamResult.TeamFull

        dao.insertMember(
            TeamMemberEntity(
                teamId = teamId,
                pokemonId = pokemonId,
                pokemonName = pokemonName,
                pokemonImageUrl = pokemonImageUrl,
                position = count + 1,
                note = note,
            )
        )
        AddToTeamResult.Success
    }

    /** Удаляет участника и сдвигает позиции, чтобы не было дыр: 1, 2, 3… */
    suspend fun removeMember(teamId: Long, pokemonId: Int) = addMutex.withLock {
        dao.removeMember(teamId, pokemonId)
        dao.getMembers(teamId)
            .sortedBy { it.position }
            .forEachIndexed { index, member ->
                if (member.position != index + 1) {
                    dao.updateMember(member.copy(position = index + 1))
                }
            }
    }

    suspend fun updateNote(teamId: Long, pokemonId: Int, note: String) {
        dao.getMembers(teamId)
            .firstOrNull { it.pokemonId == pokemonId }
            ?.let { dao.updateMember(it.copy(note = note.trim())) }
    }

    companion object {
        const val MAX_TEAM_SIZE = 6
    }
}

private fun TeamEntity.toTeam(memberCount: Int) = Team(
    id = id,
    name = name,
    memberCount = memberCount,
    createdAt = createdAt,
)

private fun TeamMemberEntity.toMember() = TeamMember(
    pokemonId = pokemonId,
    name = pokemonName,
    imageUrl = pokemonImageUrl,
    position = position,
    note = note,
    addedAt = addedAt,
)