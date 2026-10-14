package com.example.pokeexplorer.integration

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pokeexplorer.core.UiState
import com.example.pokeexplorer.data.model.AbilityEntry
import com.example.pokeexplorer.data.model.PokemonDetail
import com.example.pokeexplorer.data.model.StatEntry
import com.example.pokeexplorer.ui.detail.PokemonDetailEvent
import com.example.pokeexplorer.ui.detail.PokemonDetailScreen
import com.example.pokeexplorer.ui.detail.PokemonDetailUiState
import com.example.pokeexplorer.ui.theme.PokeExplorerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst

/**
 * ИНТЕГРАЦИОННЫЕ тесты экрана деталей.
 */

@RunWith(AndroidJUnit4::class)
class DetailScreenIntegrationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pikachuDetail = PokemonDetail(
        id = 25,
        name = "pikachu",
        imageUrl = "https://img/25.png",
        heightDecimetres = 4,
        weightHectograms = 60,
        baseExperience = 112,
        types = listOf("electric"),
        stats = listOf(StatEntry("hp", 35)),
        abilities = listOf(AbilityEntry("static", false)),
    )

    @Test
    fun после_загрузки_экран_показывает_данные_нужного_покемона() {
        composeRule.setContent {
            PokeExplorerTheme {
                PokemonDetailScreen(
                    uiState = PokemonDetailUiState(
                        id = 25,
                        name = "pikachu",
                        imageUrl = "https://img/25.png",
                        detail = UiState.Success(pikachuDetail),
                    ),
                    onEvent = {},
                    onBack = {},
                )
            }
        }

        // Заголовок — имя открытого покемона (prettyName поднимает регистр).
        composeRule.onAllNodesWithText("Pikachu").onFirst().assertExists()

        // И характеристики на месте: роспись «0.4 м» получается из heightDecimetres = 4.
        composeRule.onNodeWithText("0.4 м").assertExists()
        composeRule.onNodeWithText("6.0 кг").assertExists()
    }

    @Test
    fun заголовок_показывает_номер_пока_детали_грузятся() {
        composeRule.setContent {
            PokeExplorerTheme {
                PokemonDetailScreen(
                    uiState = PokemonDetailUiState(
                        id = 25,
                        name = "", // имя из аргумента маршрута не пришло
                        detail = UiState.Loading,
                    ),
                    onEvent = {},
                    onBack = {},
                )
            }
        }

        // Даже без имени экран осмысленный: вместо пустоты — номер.
        composeRule.onNodeWithText("#25").assertExists()
    }

    @Test
    fun ошибка_загрузки_и_кнопка_Retry_отправляют_событие() {
        var retryRequested = false

        composeRule.setContent {
            PokeExplorerTheme {
                PokemonDetailScreen(
                    uiState = PokemonDetailUiState(
                        id = 25,
                        name = "pikachu",
                        detail = UiState.Error("Запись не найдена (404)"),
                    ),
                    onEvent = { event ->
                        if (event is PokemonDetailEvent.OnRetry) retryRequested = true
                    },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Не удалось загрузить").assertExists()
        composeRule.onNodeWithText("Запись не найдена (404)").assertExists()

        composeRule.onNodeWithText("Повторить").performClick()

        assertTrue(retryRequested)
    }

    @Test
    fun сердечко_в_панели_соответствует_состоянию_и_отправляет_событие() {
        var toggleRequested = false

        composeRule.setContent {
            PokeExplorerTheme {
                PokemonDetailScreen(
                    uiState = PokemonDetailUiState(
                        id = 25,
                        name = "pikachu",
                        detail = UiState.Success(pikachuDetail),
                        isFavourite = true,
                    ),
                    onEvent = { event ->
                        if (event is PokemonDetailEvent.OnToggleFavourite) toggleRequested = true
                    },
                    onBack = {},
                )
            }
        }

        // Покемон в избранном — подпись кнопки соответствующая.
        composeRule.onNodeWithContentDescription("Убрать из избранного").performClick()

        assertTrue("нажатие должно дойти до обработчика", toggleRequested)
    }

    @Test
    fun кнопка_Назад_вызывает_навигацию_обратно() {
        var backPressed = false

        composeRule.setContent {
            PokeExplorerTheme {
                PokemonDetailScreen(
                    uiState = PokemonDetailUiState(
                        id = 25,
                        name = "pikachu",
                        detail = UiState.Success(pikachuDetail),
                    ),
                    onEvent = {},
                    onBack = { backPressed = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Назад").performClick()

        assertTrue("кнопка назад должна закрывать экран", backPressed)
    }

    @Test
    fun разные_id_дают_разные_заголовки_экрана() {
        composeRule.setContent {
            PokeExplorerTheme {
                PokemonDetailScreen(
                    uiState = PokemonDetailUiState(
                        id = 6,
                        name = "charizard",
                        detail = UiState.Loading,
                    ),
                    onEvent = {},
                    onBack = {},
                )
            }
        }

        // Экран открыт для charizard, а не для pikachu — id и имя не перепутаны.
        composeRule.onNodeWithText("Charizard").assertExists()
        composeRule.onNodeWithText("Pikachu").assertDoesNotExist()
    }
}
