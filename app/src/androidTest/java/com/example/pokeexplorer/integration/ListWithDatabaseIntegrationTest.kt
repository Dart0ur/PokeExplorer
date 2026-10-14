package com.example.pokeexplorer.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pokeexplorer.core.UiState
import com.example.pokeexplorer.data.favourites.RoomFavouritesRepository
import com.example.pokeexplorer.data.local.PokeDatabase
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.ui.list.PokemonListEvent
import com.example.pokeexplorer.ui.list.PokemonListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ИНТЕГРАЦИОННЫЙ тест связки ViewModel + Repository + Room.
 *
 * Проверяет то, что по отдельности не проверить: экран работает с настоящей базой.
 * Клик по сердечку в списке должен не просто изменить состояние на экране,
 * а реально записать строку в таблицу — и после «нового чтения» из базы
 * состояние поднимется обратно в UI.
 *
 * Важно: Room выполняет запросы на СВОИХ потоках, а не на тестовом диспетчере,
 * поэтому advanceUntilIdle() их не дожидается. Вместо «проверить value сразу»
 * тест ждёт нужного состояния через uiState.first { ... }.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ListWithDatabaseIntegrationTest {

    private lateinit var database: PokeDatabase
    private lateinit var favouritesRepository: RoomFavouritesRepository
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PokeDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        database.insertTestProfile(1)
        favouritesRepository = RoomFavouritesRepository(
            database.favouriteDao(),
            TestActiveProfileProvider(initial = 1L),
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    /**
     * Создаёт ViewModel и держит подписку на uiState весь тест.
     * Без подписчика stateIn(WhileSubscribed) не запускает загрузку.
     * backgroundScope сам отменит подписку в конце теста.
     */
    private fun TestScope.createSubscribedViewModel(): PokemonListViewModel {
        val viewModel = PokemonListViewModel(
            repository = FakePokemonRepository(),
            favouritesRepository = favouritesRepository,
        )
        backgroundScope.launch { viewModel.uiState.collect { } }
        return viewModel
    }

    @Test
    fun событие_избранного_из_списка_доходит_до_базы_и_возвращается_в_состояние() = runTest(dispatcher) {
        val viewModel = createSubscribedViewModel()
        advanceUntilIdle()

        // Ждём, пока список загрузится (а не проверяем «прямо сейчас»).
        val loaded = viewModel.uiState.first { it.content is UiState.Success<*> }
        assertTrue("список загружен", loaded.content is UiState.Success<*>)
        assertTrue("на старте избранное пустое", loaded.favouriteIds.isEmpty())

        val pikachu = PokemonListItem(25, "pikachu", "https://img/25.png")
        viewModel.onEvent(PokemonListEvent.OnToggleFavourite(pikachu))
        advanceUntilIdle()

        // 1. Состояние экрана обновилось — ждём, пока Room пришлёт новое значение.
        val afterAdd = viewModel.uiState.first { 25 in it.favouriteIds }
        assertEquals(setOf(25), afterAdd.favouriteIds)

        // 2. Строка реально легла в таблицу — читаем её заново из базы.
        val fromDatabase = favouritesRepository.favourites.first()
        assertEquals(
            "в Room должна лежать именно та запись, которую добавили",
            listOf(pikachu),
            fromDatabase,
        )

        // 3. Повторное нажатие снимает запись, и в базе тоже пусто.
        viewModel.onEvent(PokemonListEvent.OnToggleFavourite(pikachu))
        advanceUntilIdle()

        val afterRemove = viewModel.uiState.first { it.favouriteIds.isEmpty() }
        assertTrue(afterRemove.favouriteIds.isEmpty())
        assertEquals(emptyList<PokemonListItem>(), favouritesRepository.favourites.first())
    }

    @Test
    fun состояние_избранного_поднимается_из_базы_при_старте_новой_модели() = runTest(dispatcher) {
        // Данные оказались в базе до того, как экран был создан.
        favouritesRepository.toggle(PokemonListItem(6, "charizard", "https://img/6.png"))

        val viewModel = createSubscribedViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.favouriteIds.isNotEmpty() }
        assertEquals(
            "новая ViewModel должна подхватить уже сохранённое избранное",
            setOf(6),
            state.favouriteIds,
        )
    }
}