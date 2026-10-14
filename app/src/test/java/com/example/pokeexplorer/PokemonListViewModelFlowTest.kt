package com.example.pokeexplorer

import app.cash.turbine.test
import com.example.pokeexplorer.core.UiState
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.fake.FakeFavouritesRepository
import com.example.pokeexplorer.fake.FakePokemonRepository
import com.example.pokeexplorer.ui.list.FilterMode
import com.example.pokeexplorer.ui.list.PokemonListEvent
import com.example.pokeexplorer.ui.list.PokemonListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.test.advanceUntilIdle
import com.example.pokeexplorer.fake.FakeHiddenRepository
/**
 * Юнит-тесты реактивного списка (ДЗ №6: Flow/StateFlow/SharedFlow).
 *
 * Ключевое отличие от ДЗ №4: теперь uiState — StateFlow, а не обычное поле.
 * Проверяем через Turbine: подписываемся на поток, двигаем время, ловим эмиссии.
 *
 * Это закрывает требование ДЗ №5 про «последовательность эмиссий, а не только state.value».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PokemonListViewModelFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakePokemonRepository = FakePokemonRepository(),
        favourites: FakeFavouritesRepository = FakeFavouritesRepository(),
    ) = PokemonListViewModel(repository, favourites)

    // -------------------------------------------------------- начальное состояние

    @Test
    fun `начальное состояние — Loading, первая эмиссия запускается сразу`() = runTest {
        val repository = FakePokemonRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            // Первая эмиссия: Loading.
            val initial = awaitItem()
            assertEquals(UiState.Loading, initial.content)
            assertEquals("", initial.query)
            assertEquals(FilterMode.ALL, initial.filterMode)

            // Прокручиваем все оставшиеся корутины — поток идёт в сеть.
            advanceUntilIdle()

            // Вторая эмиссия: Success с данными.
            val loaded = awaitItem()
            assertTrue("ожидали Success, получили ${loaded.content}", loaded.content is UiState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ----------------------------------------------------------- успех и ошибка

    @Test
    fun `успешная загрузка эмитит Success с данными первой страницы`() = runTest {
        val repository = FakePokemonRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem().content)
            advanceUntilIdle()

            val success = awaitItem()
            assertTrue(success.content is UiState.Success)
            val data = (success.content as UiState.Success).data
            assertEquals(listOf(25, 6), data.items.map { it.id })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ошибка сети эмитит Error, retry эмитит Loading затем Success`() = runTest {
        val repository = FakePokemonRepository().apply {
            pageResult = Result.failure(java.net.UnknownHostException("offline"))
        }
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem().content)
            advanceUntilIdle()

            val error = awaitItem()
            assertTrue("ожидали Error, получили ${error.content}", error.content is UiState.Error)

            // «Чиним» сеть.
            repository.pageResult = FakePokemonRepository.DEFAULT_PAGE_RESULT
            viewModel.onEvent(PokemonListEvent.OnRetry)

            // Retry должен эмитить Loading, затем Success.
            assertEquals(UiState.Loading, awaitItem().content)
            advanceUntilIdle()

            val retrySuccess = awaitItem()
            assertTrue("retry привёл к Success", retrySuccess.content is UiState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --------------------------------------------------------------------- поиск

    @Test
    fun `быстрый набор текста — debounce отменяет предыдущий запрос, в сеть уходит только последний`() = runTest {
        val repository = FakePokemonRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            skipItems(1) // начальный Loading
            advanceUntilIdle()
            skipItems(1) // первая страница

            repository.indexCalls = 0 // сбрасываем счётчик

            // Печатаем «char» по буквам быстрее debounce.
            viewModel.onEvent(PokemonListEvent.OnQueryChange("c"))
            advanceTimeBy(100)
            viewModel.onEvent(PokemonListEvent.OnQueryChange("cha"))
            advanceTimeBy(100)
            viewModel.onEvent(PokemonListEvent.OnQueryChange("char"))

            // Ждём debounce и завершение запроса.
            advanceTimeBy(500)
            advanceUntilIdle()

            // В сеть ушли один раз — за последним запросом.
            assertEquals("debounce должен отменить первые два", 1, repository.indexCalls)

            // На экране результат последнего запроса.
            val result = expectMostRecentItem()
            assertTrue(result.content is UiState.Success)
            val names = (result.content as UiState.Success).data.items.map { it.name }
            assertEquals(listOf("charmander", "charizard"), names)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `пустой результат поиска эмитит Empty, а не Success с пустым списком`() = runTest {
        val repository = FakePokemonRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            skipItems(1)

            viewModel.onEvent(PokemonListEvent.OnQueryChange("nosuchmon"))
            advanceTimeBy(500)
            advanceUntilIdle()

            val result = expectMostRecentItem()
            assertEquals("Empty и Success(emptyList()) — разные состояния", UiState.Empty, result.content)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --------------------------------------------------------------- фильтр

    @Test
    fun `фильтр FAVOURITES_ONLY показывает только отмеченные покемоны`() = runTest {
        val favourites = FakeFavouritesRepository()
        favourites.toggle(PokemonListItem(25, "pikachu", ""))
        favourites.toggle(PokemonListItem(609, "chandelure", ""))

        val viewModel = createViewModel(favourites = favourites)

        viewModel.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            skipItems(1)

            viewModel.onEvent(PokemonListEvent.OnFilterChange(FilterMode.FAVOURITES_ONLY))
            advanceUntilIdle()

            val filtered = expectMostRecentItem()
            assertTrue(filtered.content is UiState.Success)
            val ids = (filtered.content as UiState.Success).data.items.map { it.id }
            assertEquals(setOf(25, 609), ids.toSet())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------- избранное

    @Test
    fun `добавление в избранное обновляет состояние списка через поток из Room`() = runTest {
        val favourites = FakeFavouritesRepository()
        val viewModel = createViewModel(favourites = favourites)

        viewModel.uiState.test {
            skipItems(1)
            advanceUntilIdle()

            val before = awaitItem()
            assertTrue("на старте избранное пустое", before.favouriteIds.isEmpty())

            viewModel.onEvent(PokemonListEvent.OnToggleFavourite(PokemonListItem(25, "pikachu", "")))
            advanceUntilIdle()

            // Ждём эмиссию от combine(... , favouritesRepository.ids).
            val after = awaitItem()
            assertEquals(setOf(25), after.favouriteIds)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------------------------------------------------- SharedFlow: refresh

    @Test
    fun `refresh эмитит Loading и перезапускает поток загрузки`() = runTest {
        val repository = FakePokemonRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            skipItems(1)

            repository.pageCalls = 0

            viewModel.onEvent(PokemonListEvent.OnRefresh)

            // Refresh должен эмитить Loading сразу.
            assertEquals(UiState.Loading, awaitItem().content)
            advanceUntilIdle()

            // Затем Success.
            val refreshed = awaitItem()
            assertTrue(refreshed.content is UiState.Success)
            assertEquals("refresh — новая попытка", 1, repository.pageCalls)

            cancelAndIgnoreRemainingEvents()
        }
    }
    // ------------------------------------------------------- скрытые покемоны

    @Test
    fun `скрытый покемон пропадает из списка без повторной загрузки`() = runTest {
        val repository = FakePokemonRepository()
        val hidden = FakeHiddenRepository()
        val viewModel = PokemonListViewModel(repository, FakeFavouritesRepository(), hidden)

        viewModel.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            val before = expectMostRecentItem()
            assertEquals(listOf(25, 6), (before.content as UiState.Success).data.items.map { it.id })

            repository.pageCalls = 0
            viewModel.onEvent(PokemonListEvent.OnToggleHidden(PokemonListItem(25, "pikachu", "")))
            advanceUntilIdle()

            val after = expectMostRecentItem()
            assertEquals(listOf(6), (after.content as UiState.Success).data.items.map { it.id })
            assertEquals(setOf(25), after.hiddenIds)
            assertEquals("скрытие — фильтр поверх данных, без запроса в сеть", 0, repository.pageCalls)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `настройка «Показывать скрытых» возвращает их в список`() = runTest {
        val hidden = FakeHiddenRepository().apply { ids.value = setOf(25) }
        val viewModel = PokemonListViewModel(FakePokemonRepository(), FakeFavouritesRepository(), hidden)

        viewModel.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            assertEquals(listOf(6), (expectMostRecentItem().content as UiState.Success).data.items.map { it.id })

            hidden.show.value = true
            advanceUntilIdle()
            assertEquals(listOf(25, 6), (expectMostRecentItem().content as UiState.Success).data.items.map { it.id })

            hidden.show.value = false
            advanceUntilIdle()
            assertEquals(listOf(6), (expectMostRecentItem().content as UiState.Success).data.items.map { it.id })

            cancelAndIgnoreRemainingEvents()
        }
    }
}
