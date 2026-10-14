package com.example.pokeexplorer

import androidx.lifecycle.SavedStateHandle
import com.example.pokeexplorer.core.UiState
import com.example.pokeexplorer.data.model.PokemonDetail
import com.example.pokeexplorer.fake.FakeFavouritesRepository
import com.example.pokeexplorer.fake.FakePokemonRepository
import com.example.pokeexplorer.navigation.Routes
import com.example.pokeexplorer.ui.detail.PokemonDetailEvent
import com.example.pokeexplorer.ui.detail.PokemonDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.UnknownHostException

/**
 * Юнит-тесты экрана деталей.
 *
 * Здесь же проверяется обработка SavedStateHandle: именно из него ViewModel
 * узнаёт, для какого покемона открыт экран.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PokemonDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * Аргументы маршрута приходят как строки (в NavHost объявлен NavType.StringType).
     * Если у вас id объявлен как IntType — замените "25" на 25.
     */
    private fun handle(id: String, name: String) = SavedStateHandle(
        mapOf(
            Routes.DETAIL_ARG_ID to id,
            Routes.DETAIL_ARG_NAME to name,
        )
    )

    private fun createViewModel(
        repository: FakePokemonRepository,
        savedStateHandle: SavedStateHandle,
        favourites: FakeFavouritesRepository = FakeFavouritesRepository(),
    ) = PokemonDetailViewModel(
        repository = repository,
        favouritesRepository = favourites,
        savedStateHandle = savedStateHandle,
    )

    @Test
    fun `id из SavedStateHandle определяет, чьи детали загружаются`() = runTest {
        val repository = FakePokemonRepository().apply {
            detailResult = Result.success(
                PokemonDetail(
                    id = 25,
                    name = "pikachu",
                    imageUrl = "https://img/25.png",
                    heightDecimetres = 4,
                    weightHectograms = 60,
                    baseExperience = 112,
                    types = listOf("electric"),
                    stats = emptyList(),
                    abilities = emptyList(),
                )
            )
        }

        val viewModel = createViewModel(repository, handle("25", "pikachu"))

        // Пока детали не пришли, заголовок уже осмысленный — имя из аргумента маршрута.
        assertEquals(25, viewModel.uiState.id)
        assertEquals("pikachu", viewModel.uiState.name)
        assertEquals(UiState.Loading, viewModel.uiState.detail)

        advanceUntilIdle()

        assertEquals("в сеть ушли ровно за этим id", listOf(25), repository.requestedDetailIds)
        assertTrue(viewModel.uiState.detail is UiState.Success)
        assertEquals(
            "pikachu",
            (viewModel.uiState.detail as UiState.Success<PokemonDetail>).data.name,
        )
    }

    @Test
    fun `битый id не роняет экран и не уходит в сеть`() = runTest {
        val repository = FakePokemonRepository()
        val viewModel = createViewModel(repository, handle("not-a-number", ""))

        advanceUntilIdle()

        val detail = viewModel.uiState.detail
        assertTrue("ожидали Error, получили $detail", detail is UiState.Error)
        assertTrue(
            "сообщение должно подсказывать, что именно не так",
            (detail as UiState.Error).message.contains("Некорректный id"),
        )
        assertEquals("на битый id в сеть ходить незачем", 0, repository.detailCalls)
    }

    @Test
    fun `ошибка загрузки деталей даёт Error, retry повторяет запрос`() = runTest {
        val repository = FakePokemonRepository().apply {
            detailResult = Result.failure(UnknownHostException("offline"))
        }
        val viewModel = createViewModel(repository, handle("25", "pikachu"))

        advanceUntilIdle()
        assertTrue(viewModel.uiState.detail is UiState.Error)
        assertEquals(1, repository.detailCalls)

        repository.detailResult = FakePokemonRepository.DEFAULT_DETAIL_RESULT
        viewModel.onEvent(PokemonDetailEvent.OnRetry)
        advanceUntilIdle()

        assertEquals("retry — новая попытка", 2, repository.detailCalls)
        assertTrue(viewModel.uiState.detail is UiState.Success)
    }

    @Test
    fun `состояние избранного подхватывается из хранилища при старте`() = runTest {
        val favourites = FakeFavouritesRepository()
        favourites.toggle(com.example.pokeexplorer.data.model.PokemonListItem(25, "pikachu", ""))

        val viewModel = createViewModel(
            repository = FakePokemonRepository(),
            savedStateHandle = handle("25", "pikachu"),
            favourites = favourites,
        )
        advanceUntilIdle()

        assertTrue("покемон уже в избранном — сердечко должно быть заполнено", viewModel.uiState.isFavourite)
    }

    @Test
    fun `нажатие на сердечко переключает состояние избранного`() = runTest {
        val favourites = FakeFavouritesRepository()
        val viewModel = createViewModel(
            repository = FakePokemonRepository(),
            savedStateHandle = handle("25", "pikachu"),
            favourites = favourites,
        )
        advanceUntilIdle()
        assertFalse(viewModel.uiState.isFavourite)

        viewModel.onEvent(PokemonDetailEvent.OnToggleFavourite)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.isFavourite)

        viewModel.onEvent(PokemonDetailEvent.OnToggleFavourite)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.isFavourite)
    }
}
