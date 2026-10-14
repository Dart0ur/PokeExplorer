package com.example.pokeexplorer

import com.example.pokeexplorer.data.favourites.RoomFavouritesRepository
import com.example.pokeexplorer.fake.FakeFavouriteDao
import com.example.pokeexplorer.data.mapper.extractId
import com.example.pokeexplorer.data.mapper.toDetail
import com.example.pokeexplorer.data.mapper.toListItem
import com.example.pokeexplorer.data.mapper.toSpeciesInfo
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.data.remote.dto.FlavorTextDto
import com.example.pokeexplorer.data.remote.dto.GenusDto
import com.example.pokeexplorer.data.remote.dto.NamedResourceDto
import com.example.pokeexplorer.data.remote.dto.PokemonDto
import com.example.pokeexplorer.data.remote.dto.PokemonSpeciesDto
import com.example.pokeexplorer.data.remote.dto.SpritesDto
import com.example.pokeexplorer.data.remote.dto.StatSlotDto
import com.example.pokeexplorer.data.remote.dto.TypeSlotDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import com.example.pokeexplorer.data.teams.FakeActiveProfileProvider
/**
 * Юнит-тесты на «логичную» часть приложения: маппинг PokeAPI
 * (включая описание из species), избранное и семантику состояния.
 */
class PokemonDataTest {

    // ------------------------------------------------------------------ маппинг

    @Test
    fun `id извлекается из url ресурса PokeAPI`() {
        val dto = NamedResourceDto(name = "pikachu", url = "https://pokeapi.co/api/v2/pokemon/25/")
        assertEquals(25, dto.extractId())
    }

    @Test
    fun `битый url не ломает список, а отбрасывается`() {
        val dto = NamedResourceDto(name = "broken", url = "https://pokeapi.co/api/v2/pokemon/not-a-number/")
        assertNull(dto.extractId())
        assertNull("Элемент без id не должен попасть в список", dto.toListItem())
    }

    @Test
    fun `арт строится по id даже если спрайтов нет`() {
        val item = NamedResourceDto(
            name = "mewtwo",
            url = "https://pokeapi.co/api/v2/pokemon/150/",
        ).toListItem()!!

        assertTrue(item.imageUrl.contains("/official-artwork/150.png"))
    }

    @Test
    fun `детали маппятся с типами по slot и спрайтом-заглушкой`() {
        val dto = PokemonDto(
            id = 25,
            name = "pikachu",
            height = 4,
            weight = 60,
            baseExperience = 112,
            sprites = SpritesDto(frontDefault = "https://sprites/25.png"),
            types = listOf(
                TypeSlotDto(slot = 2, type = NamedResourceDto("flying", "")),
                TypeSlotDto(slot = 1, type = NamedResourceDto("electric", "")),
            ),
        )

        val detail = dto.toDetail()

        assertEquals(listOf("electric", "flying"), detail.types) // отсортировано по slot
        assertEquals(4, detail.heightDecimetres)
        assertEquals(60, detail.weightHectograms)
        assertEquals(112, detail.baseExperience)
        assertEquals("https://sprites/25.png", detail.imageUrl)
        // Описания в /pokemon/{id} нет — без species его и не должно быть.
        assertNull(detail.description)
    }

    @Test
    fun `статы маппятся с базовыми значениями`() {
        val dto = PokemonDto(
            id = 1,
            name = "bulbasaur",
            stats = listOf(
                StatSlotDto(baseStat = 45, stat = NamedResourceDto("hp", "")),
                StatSlotDto(baseStat = 49, stat = NamedResourceDto("attack", "")),
            ),
        )

        assertEquals(listOf("hp", "attack"), dto.toDetail().stats.map { it.name })
        assertEquals(45, dto.toDetail().stats.first().value)
    }

    // ------------------------------------------------------- описание (species)

    @Test
    fun `выбирается английское описание, а не первое попавшееся`() {
        val species = PokemonSpeciesDto(
            id = 25,
            name = "pikachu",
            flavorTextEntries = listOf(
                FlavorTextDto(
                    flavorText = "Quando si arrabbia, libera scariche elettriche.",
                    language = NamedResourceDto("it", ""),
                ),
                FlavorTextDto(
                    flavorText = "It has small electric sacs on both its cheeks.",
                    language = NamedResourceDto("en", ""),
                ),
            ),
        )

        assertEquals(
            "It has small electric sacs on both its cheeks.",
            species.toSpeciesInfo().description,
        )
    }

    @Test
    fun `если английского нет, берётся любое доступное описание`() {
        val species = PokemonSpeciesDto(
            id = 25,
            flavorTextEntries = listOf(
                FlavorTextDto(
                    flavorText = "Cuando se enfada, libera descargas.",
                    language = NamedResourceDto("es", ""),
                ),
            ),
        )

        assertEquals("Cuando se enfada, libera descargas.", species.toSpeciesInfo().description)
    }

    @Test
    fun `без записей описание равно null, а не пустой строке`() {
        val species = PokemonSpeciesDto(id = 25, flavorTextEntries = emptyList())

        assertNull(
            "Пустое описание должно быть null — чтобы UI показал заглушку",
            species.toSpeciesInfo().description,
        )
    }

    @Test
    fun `описание и вид попадают в детали покемона`() {
        val dto = PokemonDto(id = 1, name = "bulbasaur")
        val species = PokemonSpeciesDto(
            id = 1,
            flavorTextEntries = listOf(
                FlavorTextDto(
                    flavorText = "A strange seed was planted on its back at birth.",
                    language = NamedResourceDto("en", ""),
                ),
            ),
            genera = listOf(GenusDto(genus = "Seed Pokémon", language = NamedResourceDto("en", ""))),
            captureRate = 45,
            habitat = NamedResourceDto("grassland", ""),
        ).toSpeciesInfo()

        val detail = dto.toDetail(species)

        assertEquals("A strange seed was planted on its back at birth.", detail.description)
        assertEquals("Seed Pokémon", detail.genus)
        assertEquals(45, detail.captureRate)
        assertEquals("grassland", detail.habitat)
        assertFalse(detail.isLegendary)
    }

    @Test
    fun `легендарность и мифичность переносятся в детали`() {
        val species = PokemonSpeciesDto(id = 151, name = "mew", isMythical = true).toSpeciesInfo()

        val detail = PokemonDto(id = 151, name = "mew").toDetail(species)

        assertTrue(detail.isMythical)
        assertFalse(detail.isLegendary)
    }

    @Test
    fun `детали собираются даже без species`() {
        // Реальный сценарий: /pokemon-species не ответил, характеристики пришли.
        val detail = PokemonDto(id = 25, name = "pikachu", height = 4).toDetail(species = null)

        assertEquals(4, detail.heightDecimetres)
        assertNull(detail.description)
        assertNull(detail.genus)
        assertFalse(detail.isLegendary)
    }

    // -------------------------------------------------------------- избранное
    // Раньше здесь был FavouritesStore; после перехода на Room те же контракты
    // проверяются на RoomFavouritesRepository с подменой DAO (без SQLite).

    // -------------------------------------------------------------- избранное
    // Контракты избранного проверяются на RoomFavouritesRepository с подменой
    // DAO (без SQLite). Профиль подставляется заглушкой, его можно переключать.

    private fun newRepository(profile: FakeActiveProfileProvider = FakeActiveProfileProvider()) =
        RoomFavouritesRepository(FakeFavouriteDao(), profile)

    @Test
    fun `toggle добавляет и убирает элемент`() = runBlocking {
        val repository = newRepository()
        val pikachu = PokemonListItem(25, "pikachu", "")
        val bulbasaur = PokemonListItem(1, "bulbasaur", "")

        assertFalse(repository.isFavourite(pikachu.id))
        repository.toggle(pikachu)
        assertTrue(repository.isFavourite(pikachu.id))

        repository.toggle(bulbasaur)
        assertEquals(
            "оба элемента должны быть в избранном",
            setOf(1, 25),
            repository.ids.first(),
        )

        repository.toggle(pikachu)
        assertFalse("повторный toggle снимает отметку", repository.isFavourite(pikachu.id))
        assertEquals(setOf(1), repository.ids.first())
    }

    @Test
    fun `повторное добавление не дублирует элемент`() = runBlocking {
        val repository = newRepository()
        val item = PokemonListItem(7, "squirtle", "")

        repository.toggle(item)
        repository.remove(7)
        repository.toggle(item)
        repository.toggle(item) // снова убрали
        assertEquals(0, repository.favourites.first().size)

        repository.toggle(item)
        assertEquals(1, repository.favourites.first().size)
    }

    @Test
    fun `избранное читается из хранилища повторно`() = runBlocking {
        val repository = newRepository()
        repository.toggle(PokemonListItem(25, "pikachu", ""))

        // Повторное чтение того же источника — данные на месте.
        assertEquals(setOf(25), repository.ids.first())
    }

    @Test
    fun `у каждого профиля своё избранное`() = runBlocking {
        val profile = FakeActiveProfileProvider(initial = 1L)
        val repository = newRepository(profile)
        repository.toggle(PokemonListItem(25, "pikachu", ""))

        profile.state.value = 2L
        assertTrue("во втором профиле избранное пустое", repository.ids.first().isEmpty())
        repository.toggle(PokemonListItem(1, "bulbasaur", ""))

        profile.state.value = 1L
        assertEquals(setOf(25), repository.ids.first())
    }
}
