package com.example.pokeexplorer.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.ui.detail.PokemonDetailScreen
import com.example.pokeexplorer.ui.detail.PokemonDetailViewModel
import com.example.pokeexplorer.ui.favourites.FavouritesScreen
import com.example.pokeexplorer.ui.favourites.FavouritesViewModel
import com.example.pokeexplorer.ui.history.HistoryRecorderViewModel
import com.example.pokeexplorer.ui.history.HistoryScreen
import com.example.pokeexplorer.ui.history.HistoryViewModel
import com.example.pokeexplorer.ui.list.PokemonListScreen
import com.example.pokeexplorer.ui.list.PokemonListViewModel
import com.example.pokeexplorer.ui.profile.ProfileViewModel
import com.example.pokeexplorer.ui.profile.ProfilesScreen
import com.example.pokeexplorer.ui.settings.SettingsScreen
import com.example.pokeexplorer.ui.settings.SettingsViewModel
import com.example.pokeexplorer.ui.teams.AddToTeamOverlay
import com.example.pokeexplorer.ui.teams.TeamDetailScreen
import com.example.pokeexplorer.ui.teams.TeamDetailViewModel
import com.example.pokeexplorer.ui.teams.TeamsListScreen
import com.example.pokeexplorer.ui.teams.TeamsListViewModel

/**
 * Граф навигации. ViewModel'и выдаёт hiltViewModel(), зависимости Hilt
 * подставляет в конструктор. Там, где состояние — StateFlow, экран
 * подписывается через collectAsState().
 */
@Composable
fun PokeExplorerNavHost(
    navController: NavHostController = rememberNavController(),
) {
    /** Единая точка перехода в детали: id — обязательный, name — для заголовка. */
    val openDetail: (PokemonListItem) -> Unit = { item ->
        navController.navigate(Routes.detail(item.id, item.name))
    }
    val backToList: () -> Unit = {
        navController.popBackStack(route = Routes.LIST, inclusive = false)
    }

    NavHost(navController = navController, startDestination = Routes.LIST) {

        // ------------------------------------------------------- 1. Список
        composable(route = Routes.LIST) {
            val viewModel: PokemonListViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            PokemonListScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onPokemonClick = openDetail,
                onOpenFavourites = { navController.navigate(Routes.FAVOURITES) },
                onOpenProfiles = { navController.navigate(Routes.PROFILES) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenTeams = { navController.navigate(Routes.TEAMS) },
            )
        }

        // ------------------------------------------- 2. Детали (detail/{id})
        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument(Routes.DETAIL_ARG_ID) { type = NavType.StringType },
                navArgument(Routes.DETAIL_ARG_NAME) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
            ),
        ) {
            val viewModel: PokemonDetailViewModel = hiltViewModel()
            // Записываем просмотр в историю (один раз на открытие экрана).
            hiltViewModel<HistoryRecorderViewModel>()
            Box(modifier = Modifier.fillMaxSize()) {
                PokemonDetailScreen(
                    uiState = viewModel.uiState,
                    onEvent = viewModel::onEvent,
                    onBack = { navController.popBackStack() },
                )
                // Кнопка «В команду» поверх экрана деталей.
                AddToTeamOverlay()
            }
        }

        // --------------------------------------------------- 3. Избранное
        composable(route = Routes.FAVOURITES) {
            val viewModel: FavouritesViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            FavouritesScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onPokemonClick = openDetail,
                onBack = { navController.popBackStack() },
                onGoToList = backToList,
            )
        }

        // ---------------------------------------------------- 4. Профили
        composable(route = Routes.PROFILES) {
            val viewModel: ProfileViewModel = hiltViewModel()
            ProfilesScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel,
            )
        }

        // --------------------------------------------------- 5. Настройки
        composable(route = Routes.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            SettingsScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onBack = { navController.popBackStack() },
            )
        }

        // ----------------------------------------------------- 6. История
        composable(route = Routes.HISTORY) {
            val viewModel: HistoryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            HistoryScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onPokemonClick = { item ->
                    navController.navigate(Routes.detail(item.pokemonId, item.name))
                },
                onBack = { navController.popBackStack() },
            )
        }

        // ----------------------------------------------------- 7. Команды
        composable(route = Routes.TEAMS) {
            val viewModel: TeamsListViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            TeamsListScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onTeamClick = { teamId -> navController.navigate(Routes.team(teamId)) },
                onBack = { navController.popBackStack() },
            )
        }

        // ------------------------------------------ 8. Одна команда (team/{id})
        composable(
            route = Routes.TEAM,
            arguments = listOf(
                navArgument(Routes.TEAM_ARG_ID) { type = NavType.LongType },
            ),
        ) {
            val viewModel: TeamDetailViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            TeamDetailScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onPokemonClick = { member ->
                    navController.navigate(Routes.detail(member.pokemonId, member.name))
                },
                onBack = { navController.popBackStack() },
                onGoToList = backToList,
            )
        }
    }
}