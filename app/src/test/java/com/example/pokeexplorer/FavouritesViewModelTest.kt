package com.example.pokeexplorer

import app.cash.turbine.test
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.fake.FakeFavouritesRepository
import com.example.pokeexplorer.ui.favourites.FavouritesViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Тесты потокового состояния экрана «Избранное».
 *
 * Здесь проверяется не конечное state.value, а вся последовательность эмиссий —
 * ровно то, что требуется отдельным пунктом задания для проектов на Flow.
 *
 * Диспетчер — UnconfinedTestDispatcher: эмиссия из Room-подобного Flow доходит
 * до подписчика сразу, без ручной прокрутки планировщика, поэтому порядок событий
 * проверяется один в один и не зависит от таймингов.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavouritesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val pikachu = PokemonListItem(25, "pikachu", "https://img/25.png")
    private val charizard = PokemonListItem(6, "charizard", "https://img/6.png")

    @Test
    fun `полная последовательность эмиссий при добавлении и удалении`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeFavouritesRepository()
            val viewModel = FavouritesViewModel(repository)

            viewModel.uiState.test {
                // 1. Подписка сразу получает начальное состояние.
                assertEquals(emptyList<PokemonListItem>(), awaitItem().items)

                // 2. Добавили первого.
                repository.toggle(pikachu)
                assertEquals(listOf(pikachu), awaitItem().items)

                // 3. Добавили второго — новые сверху.
                repository.toggle(charizard)
                assertEquals(listOf(charizard, pikachu), awaitItem().items)

                // 4. Убрали первого.
                repository.toggle(pikachu)
                assertEquals(listOf(charizard), awaitItem().items)

                // 5. Очистили всё.
                repository.clear()
                assertEquals(emptyList<PokemonListItem>(), awaitItem().items)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `два изменения дают ровно две эмиссии и ни одной лишней`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeFavouritesRepository()
            val viewModel = FavouritesViewModel(repository)

            viewModel.uiState.test {
                assertEquals(emptyList<PokemonListItem>(), awaitItem().items)

                repository.toggle(pikachu)
                assertEquals(listOf(pikachu), awaitItem().items)

                repository.toggle(charizard)
                assertEquals(listOf(charizard, pikachu), awaitItem().items)

                // Если бы ViewModel дублировала состояние вручную,
                // здесь прилетела бы третья, лишняя эмиссия.
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `подписка получает актуальное состояние, а не initialValue`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeFavouritesRepository()
            val viewModel = FavouritesViewModel(repository)

            // Данные появились ДО того, как кто-либо подписался на экран.
            repository.toggle(pikachu)
            repository.toggle(charizard)

            viewModel.uiState.test {
                assertEquals(
                    "первое, что увидит экран — реальное содержимое хранилища",
                    listOf(charizard, pikachu),
                    awaitItem().items,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `события экрана удаляют запись и очищают список`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeFavouritesRepository()
            val viewModel = FavouritesViewModel(repository)

            repository.toggle(pikachu)
            repository.toggle(charizard)

            viewModel.uiState.test {
                assertEquals(listOf(charizard, pikachu), awaitItem().items)

                viewModel.onEvent(
                    com.example.pokeexplorer.ui.favourites.FavouritesEvent.OnRemove(pikachu.id)
                )
                assertEquals(listOf(charizard), awaitItem().items)

                viewModel.onEvent(
                    com.example.pokeexplorer.ui.favourites.FavouritesEvent.OnClearAll
                )
                assertEquals(emptyList<PokemonListItem>(), awaitItem().items)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
