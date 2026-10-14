package com.example.pokeexplorer.data.teams

import com.example.pokeexplorer.data.local.TeamDao
import com.example.pokeexplorer.data.local.TeamEntity
import com.example.pokeexplorer.data.local.TeamMemberCount
import com.example.pokeexplorer.data.local.TeamMemberEntity
import com.example.pokeexplorer.data.profile.ActiveProfileProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** TeamDao в памяти: ведёт себя как Room, но без базы и эмулятора. */
class FakeTeamDao : TeamDao {

    private val teams = MutableStateFlow<List<TeamEntity>>(emptyList())
    private val members = MutableStateFlow<List<TeamMemberEntity>>(emptyList())
    private var nextId = 1L

    override fun observeByProfile(profileId: Long): Flow<List<TeamEntity>> =
        teams.map { list -> list.filter { it.profileId == profileId }.sortedByDescending { it.createdAt } }

    override suspend fun getById(teamId: Long): TeamEntity? =
        teams.value.firstOrNull { it.id == teamId }

    override suspend fun insertTeam(team: TeamEntity): Long {
        val id = nextId++
        teams.update { it + team.copy(id = id) }
        return id
    }

    override suspend fun updateTeam(team: TeamEntity) {
        teams.update { list -> list.map { if (it.id == team.id) team else it } }
    }

    /** Эмулирует ForeignKey.CASCADE. */
    override suspend fun deleteTeam(team: TeamEntity) {
        teams.update { list -> list.filterNot { it.id == team.id } }
        members.update { list -> list.filterNot { it.teamId == team.id } }
    }

    override fun observeMembers(teamId: Long): Flow<List<TeamMemberEntity>> =
        members.map { list -> list.filter { it.teamId == teamId }.sortedBy { it.position } }

    override suspend fun getMembers(teamId: Long): List<TeamMemberEntity> =
        members.value.filter { it.teamId == teamId }.sortedBy { it.position }

    override suspend fun getMemberCount(teamId: Long): Int =
        members.value.count { it.teamId == teamId }

    override suspend fun isMember(teamId: Long, pokemonId: Int): Boolean =
        members.value.any { it.teamId == teamId && it.pokemonId == pokemonId }

    /** REPLACE по составному ключу (teamId, pokemonId). */
    override suspend fun insertMember(member: TeamMemberEntity) {
        members.update { list ->
            list.filterNot { it.teamId == member.teamId && it.pokemonId == member.pokemonId } + member
        }
    }

    override suspend fun updateMember(member: TeamMemberEntity) {
        members.update { list ->
            list.map { if (it.teamId == member.teamId && it.pokemonId == member.pokemonId) member else it }
        }
    }

    override suspend fun removeMember(teamId: Long, pokemonId: Int) {
        members.update { list -> list.filterNot { it.teamId == teamId && it.pokemonId == pokemonId } }
    }

    override suspend fun clearMembers(teamId: Long) {
        members.update { list -> list.filterNot { it.teamId == teamId } }
    }

    override fun observeMemberCounts(profileId: Long): Flow<List<TeamMemberCount>> =
        combine(teams, members) { t, m ->
            val ids = t.filter { it.profileId == profileId }.map { it.id }.toSet()
            m.filter { it.teamId in ids }
                .groupBy { it.teamId }
                .map { (teamId, list) -> TeamMemberCount(teamId, list.size) }
        }
}

/** Профиль, который тест может переключать. */
class FakeActiveProfileProvider(initial: Long = 1L) : ActiveProfileProvider {
    val state = MutableStateFlow(initial)
    override val activeProfileId: Flow<Long> = state
}