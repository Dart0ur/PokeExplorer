package com.example.pokeexplorer.ui.teams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pokeexplorer.data.teams.Team
import com.example.pokeexplorer.data.teams.TeamRepository

/**
 * Кнопка «В команду» и выбор команды поверх экрана деталей.
 * Пустой Box не перехватывает нажатия, поэтому экран под ним работает как раньше.
 */
@Composable
fun AddToTeamOverlay(
    viewModel: AddToTeamViewModel = hiltViewModel(),
) {
    val teams by viewModel.teams.collectAsState()
    val message by viewModel.message.collectAsState()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.onMessageShown()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ExtendedFloatingActionButton(
            onClick = { showPicker = true },
            icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
            text = { Text("В команду") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_to_team"),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp),
        )
    }

    if (showPicker) {
        TeamPickerDialog(
            teams = teams,
            onPick = { teamId ->
                showPicker = false
                viewModel.addTo(teamId)
            },
            onCreate = { name ->
                showPicker = false
                viewModel.createTeamAndAdd(name)
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun TeamPickerDialog(
    teams: List<Team>,
    onPick: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить в команду") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (teams.isEmpty()) {
                    Text(
                        "Команд пока нет — создайте первую:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                teams.forEach { team ->
                    val isFull = team.memberCount >= TeamRepository.MAX_TEAM_SIZE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isFull) { onPick(team.id) }
                            .testTag("pick_team_${team.id}")
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val color = if (isFull) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                        Column(modifier = Modifier.weight(1f)) {
                            Text(team.name, style = MaterialTheme.typography.titleMedium, color = color)
                            Text(
                                "${team.memberCount}/${TeamRepository.MAX_TEAM_SIZE}" +
                                        if (isFull) " — заполнена" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Новая команда") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_team_input"),
                )
                TextButton(
                    onClick = { onCreate(newName.trim()) },
                    enabled = newName.isNotBlank(),
                    modifier = Modifier.align(Alignment.End),
                ) { Text("Создать и добавить") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
}