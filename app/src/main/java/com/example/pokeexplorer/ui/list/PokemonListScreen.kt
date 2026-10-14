package com.example.pokeexplorer.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pokeexplorer.R
import com.example.pokeexplorer.core.UiState
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.ui.common.EmptyState
import com.example.pokeexplorer.ui.common.ErrorState
import com.example.pokeexplorer.ui.common.LoadingState
import com.example.pokeexplorer.ui.common.PokemonCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonListScreen(
    uiState: PokemonListUiState,
    onEvent: (PokemonListEvent) -> Unit,
    onPokemonClick: (PokemonListItem) -> Unit,
    onOpenFavourites: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onOpenProfiles: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenTeams: () -> Unit = {},
) {
    var queryText by rememberSaveable { mutableStateOf("") }

    val clearQuery: () -> Unit = {
        queryText = ""
        onEvent(PokemonListEvent.OnQueryChange(""))
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.list_title), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { onEvent(PokemonListEvent.OnRefresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    IconButton(onClick = onOpenFavourites) {
                        Icon(Icons.Default.Favorite, contentDescription = stringResource(R.string.open_favourites))
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "История")
                    }
                    IconButton(onClick = onOpenTeams) {
                        Icon(Icons.Default.Groups, contentDescription = "Команды")
                    }
                    IconButton(onClick = onOpenProfiles) {
                        Icon(Icons.Default.Person, contentDescription = "Профили")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {

            SearchBar(
                query = queryText,
                onQueryChange = { newText ->
                    queryText = newText
                    onEvent(PokemonListEvent.OnQueryChange(newText))
                },
                onClear = clearQuery,
            )

            FilterBar(
                currentMode = uiState.filterMode,
                onFilterChange = { onEvent(PokemonListEvent.OnFilterChange(it)) },
            )

            PullToRefreshBox(
                isRefreshing = uiState.content is UiState.Loading,
                onRefresh = { onEvent(PokemonListEvent.OnRefresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val content = uiState.content) {

                    is UiState.Loading -> LoadingState(message = stringResource(R.string.loading))

                    is UiState.Error -> ErrorState(
                        message = content.message,
                        onRetry = { onEvent(PokemonListEvent.OnRetry) },
                    )

                    is UiState.Empty -> EmptyState(
                        title = stringResource(R.string.empty_title),
                        subtitle = stringResource(R.string.empty_subtitle),
                        actionLabel = stringResource(R.string.search_clear),
                        onAction = clearQuery,
                    )

                    is UiState.Success -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            items = content.data.items,
                            key = { it.id },
                        ) { item ->
                            val isHidden = item.id in uiState.hiddenIds
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    PokemonCard(
                                        item = item,
                                        isFavourite = item.id in uiState.favouriteIds,
                                        onClick = { onPokemonClick(item) },
                                        onToggleFavourite = { onEvent(PokemonListEvent.OnToggleFavourite(item)) },
                                    )
                                }
                                IconButton(
                                    onClick = { onEvent(PokemonListEvent.OnToggleHidden(item)) },
                                    modifier = Modifier.testTag("hide_${item.id}"),
                                ) {
                                    Icon(
                                        imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isHidden) "Вернуть в список" else "Скрыть",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Поле поиска. */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
    )
}

/** Панель фильтра. */
@Composable
private fun FilterBar(
    currentMode: FilterMode,
    onFilterChange: (FilterMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.filter_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FilterChip(
            selected = currentMode == FilterMode.ALL,
            onClick = { onFilterChange(FilterMode.ALL) },
            label = { Text(stringResource(R.string.filter_all)) },
        )

        FilterChip(
            selected = currentMode == FilterMode.FAVOURITES_ONLY,
            onClick = { onFilterChange(FilterMode.FAVOURITES_ONLY) },
            label = { Text(stringResource(R.string.filter_favourites)) },
        )
    }
}