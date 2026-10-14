package com.example.pokeexplorer.data.teams

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TeamRepositoryTest {

    private lateinit var dao: FakeTeamDao
    private lateinit var profile: FakeActiveProfileProvider
    private lateinit var repository: TeamRepository

    @Before
    fun setUp() {
        dao = FakeTeamDao()
        profile = FakeActiveProfileProvider(initial = 1L)
        repository = TeamRepository(dao, profile)
    }

    private suspend fun add(teamId: Long, pokemonId: Int) =
        repository.addMember(teamId, pokemonId, "poke$pokemonId", "url$pokemonId")

    @Test
    fun `первые 6 покемонов добавляются, седьмой отклоняется`() = runTest {
        val teamId = repository.createTeam("Основа")

        (1..6).forEach { assertEquals(AddToTeamResult.Success, add(teamId, it)) }
        assertEquals(AddToTeamResult.TeamFull, add(teamId, 7))

        assertEquals(TeamRepository.MAX_TEAM_SIZE, dao.getMemberCount(teamId))
    }

    @Test
    fun `позиции идут подряд с 1`() = runTest {
        val teamId = repository.createTeam("Основа")
        listOf(25, 1, 4).forEach { add(teamId, it) }

        val members = repository.observeMembers(teamId).first()

        assertEquals(listOf(1, 2, 3), members.map { it.position })
        assertEquals(listOf(25, 1, 4), members.map { it.pokemonId })
    }

    @Test
    fun `повторное добавление того же покемона возвращает AlreadyInTeam`() = runTest {
        val teamId = repository.createTeam("Основа")
        add(teamId, 25)

        assertEquals(AddToTeamResult.AlreadyInTeam, add(teamId, 25))
        assertEquals(1, dao.getMemberCount(teamId))
    }

    @Test
    fun `дубликат в полной команде — AlreadyInTeam, а не TeamFull`() = runTest {
        val teamId = repository.createTeam("Основа")
        (1..6).forEach { add(teamId, it) }

        assertEquals(AddToTeamResult.AlreadyInTeam, add(teamId, 3))
    }

    @Test
    fun `один покемон может быть в разных командах`() = runTest {
        val a = repository.createTeam("A")
        val b = repository.createTeam("B")

        assertEquals(AddToTeamResult.Success, add(a, 25))
        assertEquals(AddToTeamResult.Success, add(b, 25))
    }

    @Test
    fun `удаление участника пересчитывает позиции без дыр`() = runTest {
        val teamId = repository.createTeam("Основа")
        listOf(10, 20, 30, 40).forEach { add(teamId, it) }

        repository.removeMember(teamId, 20)

        val members = repository.observeMembers(teamId).first()
        assertEquals(listOf(10, 30, 40), members.map { it.pokemonId })
        assertEquals(listOf(1, 2, 3), members.map { it.position })
    }

    @Test
    fun `после удаления из полной команды можно добавить нового`() = runTest {
        val teamId = repository.createTeam("Основа")
        (1..6).forEach { add(teamId, it) }

        repository.removeMember(teamId, 1)

        assertEquals(AddToTeamResult.Success, add(teamId, 7))
        assertEquals(6, repository.observeMembers(teamId).first().last().position)
    }

    @Test
    fun `список команд показывает число участников`() = runTest {
        val a = repository.createTeam("A")
        val b = repository.createTeam("B")
        (1..3).forEach { add(a, it) }

        val counts = repository.teams.first().associate { it.id to it.memberCount }

        assertEquals(3, counts[a])
        assertEquals(0, counts[b])
    }

    @Test
    fun `команды переключаются вместе с профилем`() = runTest {
        repository.createTeam("Команда профиля 1")

        profile.state.value = 2L
        assertTrue(repository.teams.first().isEmpty())

        repository.createTeam("Команда профиля 2")
        assertEquals(listOf("Команда профиля 2"), repository.teams.first().map { it.name })

        profile.state.value = 1L
        assertEquals(listOf("Команда профиля 1"), repository.teams.first().map { it.name })
    }

    @Test
    fun `чужая команда не видна через observeTeam`() = runTest {
        val teamId = repository.createTeam("Профиль 1")

        profile.state.value = 2L

        assertEquals(null, repository.observeTeam(teamId).first())
    }

    @Test
    fun `название и заметка сохраняются без лишних пробелов`() = runTest {
        val teamId = repository.createTeam("  Основа  ")
        add(teamId, 25)

        repository.renameTeam(teamId, "  Финал ")
        repository.updateNote(teamId, 25, "  танк  ")

        assertEquals("Финал", repository.teams.first().single().name)
        assertEquals("танк", repository.observeMembers(teamId).first().single().note)
    }
}