package com.example.pokeexplorer.ui.teams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.pokeexplorer.data.teams.Team
import com.example.pokeexplorer.ui.common.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.pokeexplorer.data.teams.TeamRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsListScreen(
    uiState: TeamsListUiState,
    onEvent: (TeamsListEvent) -> Unit,
    onTeamClick: (Long) -> Unit,
    onBack: () -> Unit,
) {
    // Храним id, а не саму Team: rememberSaveable умеет сохранять Long,
    // а data class без Parcelable — нет (упало бы при сворачивании).
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var renameTeamId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteTeamId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Команды") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("teams_create"),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Создать команду")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                uiState.teams.isEmpty() -> EmptyState(
                    title = "Нет команд",
                    subtitle = "Нажмите + чтобы создать первую команду",
                    actionLabel = "Создать команду",
                    onAction = { showCreateDialog = true },
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("teams_list"),
                ) {
                    items(uiState.teams, key = { it.id }) { team ->
                        TeamRow(
                            team = team,
                            onClick = { onTeamClick(team.id) },
                            onRename = { renameTeamId = team.id },
                            onDelete = { deleteTeamId = team.id },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        TextInputDialog(
            title = "Новая команда",
            label = "Название команды",
            initialValue = "",
            onConfirm = { name ->
                showCreateDialog = false
                onEvent(TeamsListEvent.OnCreate(name))
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    uiState.teams.firstOrNull { it.id == renameTeamId }?.let { team ->
        TextInputDialog(
            title = "Переименовать команду",
            label = "Название",
            initialValue = team.name,
            onConfirm = { newName ->
                renameTeamId = null
                onEvent(TeamsListEvent.OnRename(team.id, newName))
            },
            onDismiss = { renameTeamId = null },
        )
    }

    uiState.teams.firstOrNull { it.id == deleteTeamId }?.let { team ->
        AlertDialog(
            onDismissRequest = { deleteTeamId = null },
            title = { Text("Удалить команду?") },
            text = { Text("«${team.name}» будет удалена со всеми покемонами.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTeamId = null
                    onEvent(TeamsListEvent.OnDelete(team.id))
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTeamId = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun TeamRow(
    team: Team,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("team_${team.id}"),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(team.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${team.memberCount}/${TeamRepository.MAX_TEAM_SIZE} покемонов · создана ${formatDate(team.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Переименовать")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.testTag("team_name_input"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
            ) { Text("ОК") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))