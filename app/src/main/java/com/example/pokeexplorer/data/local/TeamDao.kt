package com.example.pokeexplorer.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Команды и их состав. Лимит в 6 покемонов проверяет репозиторий:
 * в SQL такое правило выражается неудобно и его нельзя протестировать отдельно.
 */
@Dao
interface TeamDao {

    // --- команды ---

    @Query("SELECT * FROM teams WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observeByProfile(profileId: Long): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getById(teamId: Long): TeamEntity?

    @Insert
    suspend fun insertTeam(team: TeamEntity): Long

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Delete
    suspend fun deleteTeam(team: TeamEntity)

    // --- состав команды ---

    @Query("SELECT * FROM team_members WHERE teamId = :teamId ORDER BY position ASC")
    fun observeMembers(teamId: Long): Flow<List<TeamMemberEntity>>

    @Query("SELECT * FROM team_members WHERE teamId = :teamId ORDER BY position ASC")
    suspend fun getMembers(teamId: Long): List<TeamMemberEntity>

    @Query("SELECT COUNT(*) FROM team_members WHERE teamId = :teamId")
    suspend fun getMemberCount(teamId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM team_members WHERE teamId = :teamId AND pokemonId = :pokemonId)")
    suspend fun isMember(teamId: Long, pokemonId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: TeamMemberEntity)

    @Update
    suspend fun updateMember(member: TeamMemberEntity)

    @Query("DELETE FROM team_members WHERE teamId = :teamId AND pokemonId = :pokemonId")
    suspend fun removeMember(teamId: Long, pokemonId: Int)

    @Query("DELETE FROM team_members WHERE teamId = :teamId")
    suspend fun clearMembers(teamId: Long)

    /** Число участников во всех командах профиля. Обновляется при любом изменении состава. */
    @Query("""
        SELECT m.teamId AS teamId, COUNT(*) AS memberCount
        FROM team_members m
        INNER JOIN teams t ON t.id = m.teamId
        WHERE t.profileId = :profileId
        GROUP BY m.teamId
    """)
    fun observeMemberCounts(profileId: Long): Flow<List<TeamMemberCount>>
}

/** Результат запроса observeMemberCounts. Это не таблица, а просто класс для результата запроса. */
data class TeamMemberCount(
    val teamId: Long,
    val memberCount: Int,
)