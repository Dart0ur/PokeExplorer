package com.example.pokeexplorer.ui.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pokeexplorer.R
import com.example.pokeexplorer.data.model.PokemonListItem
import com.example.pokeexplorer.ui.common.EmptyState
import com.example.pokeexplorer.ui.common.PokemonCard

/**
 * Экран «Избранное» — отдельный route (бонус).
 * Источник данных — общая таблица favourites (Room), поэтому если во время показа
 * этого экрана убрать покемона с экрана деталей, список тут же обновится.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    uiState: FavouritesUiState,
    onEvent: (FavouritesEvent) -> Unit,
    onPokemonClick: (PokemonListItem) -> Unit,
    onBack: () -> Unit,
    onGoToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.favourites_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (!uiState.isEmpty) {
                        IconButton(onClick = { onEvent(FavouritesEvent.OnClearAll) }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.clear_all),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        if (uiState.isEmpty) {
            EmptyState(
                modifier = Modifier.padding(innerPadding),
                title = stringResource(R.string.favourites_empty_title),
                subtitle = stringResource(R.string.favourites_empty_subtitle),
                actionLabel = stringResource(R.string.go_to_list),
                onAction = onGoToList,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "count") {
                    Text(
                        text = stringResource(R.string.favourites_count, uiState.items.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                items(items = uiState.items, key = { it.id }) { item ->
                    PokemonCard(
                        item = item,
                        isFavourite = true,
                        onClick = { onPokemonClick(item) },
                        onToggleFavourite = { onEvent(FavouritesEvent.OnRemove(item.id)) },
                    )
                }
            }
        }
    }
}
