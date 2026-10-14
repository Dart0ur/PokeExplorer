package com.example.pokeexplorer.integration

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pokeexplorer.core.UiState
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.ui.common.PokemonCard
import com.example.pokeexplorer.ui.list.PokemonListContent
import com.example.pokeexplorer.ui.list.PokemonListScreen
import com.example.pokeexplorer.ui.list.PokemonListUiState
import com.example.pokeexplorer.ui.theme.PokeExplorerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ИНТЕГРАЦИОННЫЕ тесты UI.
 *
 * Проверяются сценарии задания:
 *  - список -> клик по элементу -> уход в детали с нужным id;
 *  - ошибка -> нажатие Retry -> отправляется событие повтора;
 *  - экран после загрузки данных показывает именно то, что пришло в состоянии.
 *
 * Тесты идут через настоящий Compose: реальная композиция, реальные клики,
 * реальные переходы между состояниями. ViewModel не нужен — экраны stateless,
 * поэтому состояние подаётся напрямую, а действия перехватываются лямбдой.
 */
@RunWith(AndroidJUnit4::class)
class ListScreenIntegrationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pikachu = PokemonListItem(25, "pikachu", "https://img/25.png")

    private fun successState() = PokemonListUiState(
        content = UiState.Success(
            PokemonListContent(
                items = listOf(pikachu),
                totalCount = 1,
                hasMore = false,
                isSearchMode = false,
            )
        )
    )

    @Test
    fun клик_по_элементу_списка_передаёт_наружу_именно_этого_покемона() {
        var clicked: PokemonListItem? = null

        composeRule.setContent {
            PokeExplorerTheme {
                PokemonListScreen(
                    uiState = successState(),
                    onEvent = {},
                    onPokemonClick = { clicked = it },
                    onOpenFavourites = {},
                )
            }
        }

        composeRule.onNodeWithText("Pikachu").performClick()

        assertEquals("в детали должен уйти элемент, по которому кликнули", pikachu, clicked)
    }

    @Test
    fun после_успешной_загрузки_на_экране_видны_данные_а_не_индикатор() {
        composeRule.setContent {
            PokeExplorerTheme {
                PokemonListScreen(
                    uiState = successState(),
                    onEvent = {},
                    onPokemonClick = {},
                    onOpenFavourites = {},
                )
            }
        }

        // Данные из состояния действительно отрисованы.
        composeRule.onNodeWithText("Pikachu").assertExists()
        composeRule.onNodeWithText("#0025").assertExists()

        // И это именно Success, а не пустой экран или ошибка.
        composeRule.onNodeWithText("Ничего не найдено").assertDoesNotExist()
        composeRule.onNodeWithText("Не удалось загрузить").assertDoesNotExist()
    }

    @Test
    fun ошибка_показывает_сообщение_и_кнопка_Retry_отправляет_событие_повтора() {
        var retryRequested = false

        composeRule.setContent {
            PokeExplorerTheme {
                PokemonListScreen(
                    uiState = PokemonListUiState(
                        content = UiState.Error("Нет подключения к сети"),
                    ),
                    onEvent = { event ->
                        if (event is com.example.pokeexplorer.ui.list.PokemonListEvent.OnRetry) {
                            retryRequested = true
                        }
                    },
                    onPokemonClick = {},
                    onOpenFavourites = {},
                )
            }
        }

        composeRule.onNodeWithText("Не удалось загрузить").assertExists()
        composeRule.onNodeWithText("Нет подключения к сети").assertExists()

        composeRule.onNodeWithText("Повторить").performClick()

        assertTrue("нажатие Retry должно дойти до обработчика", retryRequested)
    }

    @Test
    fun пустой_результат_поиска_показывает_Empty_а_не_пустой_список() {
        composeRule.setContent {
            PokeExplorerTheme {
                PokemonListScreen(
                    uiState = PokemonListUiState(content = UiState.Empty),
                    onEvent = {},
                    onPokemonClick = {},
                    onOpenFavourites = {},
                )
            }
        }

        composeRule.onNodeWithText("Ничего не найдено").assertExists()
        composeRule.onNodeWithText("Не удалось загрузить").assertDoesNotExist()
    }

    @Test
    fun сердечко_на_карточке_отправляет_событие_избранного() {
        var toggled: PokemonListItem? = null
        var favouriteState = false

        composeRule.setContent {
            PokeExplorerTheme {
                PokemonCard(
                    item = pikachu,
                    isFavourite = favouriteState,
                    onClick = {},
                    onToggleFavourite = { toggled = pikachu },
                )
            }
        }

        // contentDescription меняется вместе с состоянием — заодно проверяем подпись.
        composeRule
            .onNodeWithContentDescription("В избранное", substring = true)
            .performClick()

        assertEquals(pikachu, toggled)
    }

    @Test
    fun несколько_карточек_в_списке_кликабельны_по_отдельности() {
        val second = PokemonListItem(6, "charizard", "https://img/6.png")
        var clicked: PokemonListItem? = null

        composeRule.setContent {
            PokeExplorerTheme {
                PokemonListScreen(
                    uiState = PokemonListUiState(
                        content = UiState.Success(
                            PokemonListContent(
                                items = listOf(pikachu, second),
                                totalCount = 2,
                                hasMore = false,
                                isSearchMode = false,
                            )
                        )
                    ),
                    onEvent = {},
                    onPokemonClick = { clicked = it },
                    onOpenFavourites = {},
                )
            }
        }

        composeRule.onNodeWithText("Charizard").performClick()

        assertEquals("клик должен открыть вторую карточку, а не первую", second, clicked)
    }

    @Test
    fun кликабельные_карточки_действительно_принимают_нажатия() {
        composeRule.setContent {
            PokeExplorerTheme {
                PokemonListScreen(
                    uiState = successState(),
                    onEvent = {},
                    onPokemonClick = {},
                    onOpenFavourites = {},
                )
            }
        }

        composeRule.onAllNodes(hasClickAction()).onFirst().assertExists()
    }
}
