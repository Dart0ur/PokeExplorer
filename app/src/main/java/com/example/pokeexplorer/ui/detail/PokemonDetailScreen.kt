package com.example.pokeexplorer.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pokeexplorer.R
import com.example.pokeexplorer.core.UiState
import com.example.pokeexplorer.data.model.PokemonDetail
import com.example.pokeexplorer.ui.common.ErrorState
import com.example.pokeexplorer.ui.common.LoadingState
import com.example.pokeexplorer.ui.theme.prettyName
import com.example.pokeexplorer.ui.theme.statLabel
import com.example.pokeexplorer.ui.theme.typeColor
import com.example.pokeexplorer.ui.theme.typeLabel
import androidx.compose.material.icons.filled.CloudOff
import com.example.pokeexplorer.data.cache.CachePolicy

/**
 * Экран деталей. Тоже stateless: рисует то, что лежит в [uiState],
 * а нажатия отдаёт наружу через [onEvent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(
    uiState: PokemonDetailUiState,
    onEvent: (PokemonDetailEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.name.takeIf { it.isNotBlank() }?.let(::prettyName)
                            ?: "#${uiState.id}",
                        fontWeight = FontWeight.Bold,
                    )
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
                    IconButton(onClick = { onEvent(PokemonDetailEvent.OnToggleFavourite) }) {
                        Icon(
                            imageVector = if (uiState.isFavourite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = stringResource(
                                if (uiState.isFavourite) {
                                    R.string.remove_from_favourites
                                } else {
                                    R.string.add_to_favourites
                                }
                            ),
                        )
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
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState.detail) {
                is UiState.Loading -> LoadingState(message = stringResource(R.string.loading))

                is UiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { onEvent(PokemonDetailEvent.OnRetry) },
                )

                is UiState.Empty -> ErrorState(
                    message = stringResource(R.string.error_unknown),
                    onRetry = { onEvent(PokemonDetailEvent.OnRetry) },
                )

                is UiState.Success -> DetailContent(
                    detail = state.data,
                    heroImageUrl = uiState.imageUrl,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: PokemonDetail,
    heroImageUrl: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        detail.offlineSince?.let { OfflineBanner(fetchedAt = it) }

        HeroCard(detail = detail, heroImageUrl = heroImageUrl)

        // Описание из /pokemon-species — то, чего нет в /pokemon/{id}.
        DescriptionCard(description = detail.description)

        // Дополнительные сведения из species; показываем только если они есть.
        if (detail.habitat != null || detail.captureRate != null) {
            SectionCard(title = stringResource(R.string.section_info)) {
                detail.habitat?.let {
                    LabeledRow(label = stringResource(R.string.label_habitat), value = prettyName(it))
                }
                detail.captureRate?.let {
                    LabeledRow(label = stringResource(R.string.label_capture_rate), value = it.toString())
                }
            }
        }

        SectionCard(title = stringResource(R.string.section_stats)) {
            detail.stats.forEach { stat ->
                StatRow(
                    label = statLabel(stat.name),
                    value = stat.value,
                    maxValue = 255,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        SectionCard(title = stringResource(R.string.section_abilities)) {
            if (detail.abilities.isEmpty()) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                detail.abilities.forEach { ability ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = prettyName(ability.name),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (ability.isHidden) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = stringResource(R.string.hidden_ability),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Текстовое описание покемона (flavor text из игр). */
@Composable
private fun DescriptionCard(description: String?) {
    SectionCard(title = stringResource(R.string.section_description)) {
        if (description.isNullOrBlank()) {
            // Мягкая деградация: характеристики всё равно показаны выше.
            Text(
                text = stringResource(R.string.descr_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.descr_english_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeroCard(
    detail: PokemonDetail,
    heroImageUrl: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = heroImageUrl,
                    contentDescription = detail.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(150.dp),
                )
            }

            Text(
                text = prettyName(detail.name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            // Вид («Seed Pokémon») + номер — из species.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "#${detail.id.toString().padStart(4, '0')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                detail.genus?.let { genus ->
                    Text(
                        text = "· ${genus.removeSuffix(" Pokémon")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Чипы типов — цвет берётся из таблицы типов.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                detail.types.forEach { type ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = typeColor(type),
                    ) {
                        Text(
                            text = typeLabel(type),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            if (detail.isLegendary || detail.isMythical) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(
                                if (detail.isMythical) R.string.mythical else R.string.legendary
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // PokeAPI отдаёт рост в дециметрах, вес — в гектограммах: переводим в м и кг.
                InfoColumn(
                    label = stringResource(R.string.label_height),
                    value = "%.1f м".format(detail.heightDecimetres / 10.0),
                )
                InfoColumn(
                    label = stringResource(R.string.label_weight),
                    value = "%.1f кг".format(detail.weightHectograms / 10.0),
                )
                InfoColumn(
                    label = stringResource(R.string.label_base_exp),
                    value = detail.baseExperience?.toString() ?: "—",
                )
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: Int,
    maxValue: Int,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { (value.toFloat() / maxValue).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
/** Плашка: сети нет, показаны сохранённые данные. */
@Composable
private fun OfflineBanner(fetchedAt: Long) {
    val hours = CachePolicy.ageHours(fetchedAt, System.currentTimeMillis())
    val ageText = when {
        hours < 1 -> "меньше часа назад"
        hours < 48 -> "$hours ч назад"
        else -> "${hours / 24} дн. назад"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = "Офлайн · данные обновлены $ageText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}