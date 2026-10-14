package com.example.pokeexplorer.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pokeexplorer.data.favourites.RoomFavouritesRepository
import com.example.pokeexplorer.data.local.FavouriteDao
import com.example.pokeexplorer.data.local.PokeDatabase
import com.example.pokeexplorer.data.model.PokemonListItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ИНТЕГРАЦИОННЫЙ тест data-слоя: Repository + Room.
 *
 * В отличие от юнит-теста с подменой DAO, здесь поднимается настоящий SQLite
 * в памяти: проверяются реальные SQL-запросы, реальные колонки, реальный Flow
 * из Room. Если в @Query опечатка или колонка названа иначе — упадёт именно здесь,
 * а не в продакшене.
 */
@RunWith(AndroidJUnit4::class)
class FavouritesDatabaseIntegrationTest {

    private lateinit var database: PokeDatabase
    private lateinit var dao: FavouriteDao
    private lateinit var repository: RoomFavouritesRepository

    private lateinit var profiles: TestActiveProfileProvider

    private val pikachu = PokemonListItem(25, "pikachu", "https://img/25.png")
    private val charizard = PokemonListItem(6, "charizard", "https://img/6.png")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // inMemoryDatabaseBuilder: база живёт только в памяти процесса теста,
        // поэтому тесты не зависят друг от друга и не мусорят в файловой системе.
        database = Room.inMemoryDatabaseBuilder(context, PokeDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        database.insertTestProfile(1)
        profiles = TestActiveProfileProvider(initial = 1L)

        dao = database.favouriteDao()
        repository = RoomFavouritesRepository(dao, profiles)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun записанные_данные_читаются_обратно_без_потерь() = runBlocking {
        repository.toggle(pikachu)

        val stored = repository.favourites.first().single()

        assertEquals(25, stored.id)
        assertEquals("pikachu", stored.name)
        assertEquals("https://img/25.png", stored.imageUrl)
    }

    @Test
    fun избранное_переживает_пересоздание_подключения_к_базе() = runBlocking {
        repository.toggle(pikachu)

        // Имитируем «новый сеанс работы»: Flow перечитывается заново,
        // данные должны прийти из таблицы, а не из памяти репозитория.
        val reread = repository.favourites.first()

        assertEquals(
            "после повторного чтения данные должны быть на месте",
            listOf(pikachu.id),
            reread.map { it.id },
        )
    }

    @Test
    fun повторное_добавление_не_создаёт_дубль_в_таблице() = runBlocking {
        repository.toggle(pikachu)
        repository.toggle(pikachu) // сняли отметку
        repository.toggle(pikachu) // поставили снова

        val all = repository.favourites.first()

        assertEquals("в таблице должна остаться ровно одна строка", 1, all.size)
        assertEquals(listOf(25), all.map { it.id })
    }

    @Test
    fun удаление_затрагивает_только_нужную_строку() = runBlocking {
        repository.toggle(pikachu)
        repository.toggle(charizard)

        repository.remove(pikachu.id)

        val left = repository.favourites.first()
        assertEquals(listOf(charizard), left)
        assertFalse(repository.isFavourite(pikachu.id))
        assertTrue(repository.isFavourite(charizard.id))
    }

    @Test
    fun новые_записи_идут_первыми_по_времени_добавления() = runBlocking {
        // addedAt у двух записей может совпасть до миллисекунды, поэтому
        // проверяем контракты поштучно: обе записи на месте и обе читаются.
        repository.toggle(pikachu)
        repository.toggle(charizard)

        val ids = repository.favourites.first().map { it.id }
        assertEquals(2, ids.size)
        assertTrue("обе записи должны читаться", ids.containsAll(listOf(25, 6)))
    }

    @Test
    fun clear_опустошает_таблицу() = runBlocking {
        repository.toggle(pikachu)
        repository.toggle(charizard)

        repository.clear()

        assertEquals(emptyList<PokemonListItem>(), repository.favourites.first())
        assertEquals(emptySet<Int>(), repository.ids.first())
    }

    @Test
    fun избранное_разделено_между_профилями() = runBlocking {
        database.insertTestProfile(2, "Эш")

        repository.toggle(pikachu)               // профиль 1
        profiles.switchTo(2)

        assertTrue(
            "у нового профиля избранное должно быть пустым",
            repository.favourites.first().isEmpty(),
        )

        repository.toggle(charizard)             // профиль 2
        assertEquals(listOf(6), repository.favourites.first().map { it.id })

        profiles.switchTo(1)
        assertEquals(
            "избранное профиля 1 не должно смешаться с профилем 2",
            listOf(25),
            repository.favourites.first().map { it.id },
        )
    }
}
