package com.example.pokeexplorer

import com.example.pokeexplorer.data.favourites.RoomFavouritesRepository
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.fake.FakeActiveProfileProvider
import com.example.pokeexplorer.fake.FakeFavouriteDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты слоя данных: репозиторий избранного поверх подмены DAO.
 *
 * Быстро (без SQLite) проверяем бизнес-правила: маппинг между Entity и доменной
 * моделью, обратимость toggle, порядок «новые сверху» и разделение по профилям.
 * Что Room реально пишет и читает — проверяется отдельным интеграционным тестом
 * на настоящей базе в памяти.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomFavouritesRepositoryTest {

    private val dao = FakeFavouriteDao()
    private val profiles = FakeActiveProfileProvider(initial = 1L)
    private val repository = RoomFavouritesRepository(dao, profiles)

    private val pikachu = PokemonListItem(25, "pikachu", "https://img/25.png")
    private val charizard = PokemonListItem(6, "charizard", "https://img/6.png")

    @Test
    fun `toggle добавляет покемона и раскладывает поля по колонкам`() = runTest {
        assertFalse(repository.isFavourite(25))

        repository.toggle(pikachu)

        assertTrue(repository.isFavourite(25))

        val stored = repository.favourites.first()
        assertEquals(1, stored.size)
        val item = stored.single()
        assertEquals("id не должен теряться", 25, item.id)
        assertEquals("имя не должно теряться", "pikachu", item.name)
        assertEquals("ссылка на арт не должна теряться", "https://img/25.png", item.imageUrl)

        // В сущность записалось то же самое — проверяем через то, что попало в DAO.
        val entity = dao.inserted.single()
        assertEquals(25, entity.id)
        assertEquals("pikachu", entity.name)
        assertEquals("запись должна принадлежать активному профилю", 1L, entity.profileId)
        assertTrue("время добавления должно быть проставлено", entity.addedAt > 0)
    }

    @Test
    fun `повторный toggle убирает покемона из избранного`() = runTest {
        repository.toggle(pikachu)
        assertTrue(repository.isFavourite(25))

        repository.toggle(pikachu)

        assertFalse("toggle обратим: второе нажатие снимает отметку", repository.isFavourite(25))
        assertEquals(emptyList<PokemonListItem>(), repository.favourites.first())
    }

    @Test
    fun `ids совпадают с содержимым favourites`() = runTest {
        repository.toggle(pikachu)
        repository.toggle(charizard)

        assertEquals(
            "поток id должен быть согласован с потоком элементов",
            repository.favourites.first().map { it.id }.toSet(),
            repository.ids.first(),
        )
        assertEquals(setOf(25, 6), repository.ids.first())
    }

    @Test
    fun `remove удаляет только указанную запись`() = runTest {
        repository.toggle(pikachu)
        repository.toggle(charizard)

        repository.remove(25)

        assertEquals(listOf(charizard), repository.favourites.first())
        assertFalse(repository.isFavourite(25))
        assertTrue(repository.isFavourite(6))
    }

    @Test
    fun `clear очищает избранное целиком`() = runTest {
        repository.toggle(pikachu)
        repository.toggle(charizard)

        repository.clear()

        assertEquals(emptyList<PokemonListItem>(), repository.favourites.first())
        assertEquals(emptySet<Int>(), repository.ids.first())
    }

    @Test
    fun `в списке нет дублей при добавлении одного и того же покемона дважды`() = runTest {
        // Добавляем, снимаем и добавляем снова — в хранилище должна остаться одна запись.
        repository.toggle(pikachu)
        repository.toggle(pikachu)
        repository.toggle(pikachu)

        val stored = repository.favourites.first()
        assertEquals("дубликатов быть не должно", 1, stored.size)
        assertEquals(listOf(25), stored.map { it.id })
    }

    // ------------------------------------------------ Итерация 2: профили

    @Test
    fun `избранное разных профилей не смешивается`() = runTest {
        repository.toggle(pikachu)          // профиль 1
        profiles.switchTo(2L)

        assertTrue("у профиля 2 избранное пустое", repository.favourites.first().isEmpty())
        assertFalse("pikachu не должен быть в избранном профиля 2", repository.isFavourite(25))

        repository.toggle(charizard)        // профиль 2
        assertEquals(listOf(6), repository.favourites.first().map { it.id })

        profiles.switchTo(1L)
        assertEquals(
            "у профиля 1 остался только pikachu",
            listOf(25),
            repository.favourites.first().map { it.id },
        )
    }

    @Test
    fun `clear очищает только активный профиль`() = runTest {
        repository.toggle(pikachu)          // профиль 1
        profiles.switchTo(2L)
        repository.toggle(charizard)        // профиль 2

        repository.clear()                  // чистим профиль 2

        assertTrue(repository.favourites.first().isEmpty())
        profiles.switchTo(1L)
        assertEquals("избранное профиля 1 не пострадало", listOf(25), repository.favourites.first().map { it.id })
    }
}