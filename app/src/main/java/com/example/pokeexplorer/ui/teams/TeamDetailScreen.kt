package com.example.pokeexplorer.ui.teams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pokeexplorer.data.teams.TeamMember
import com.example.pokeexplorer.data.teams.TeamRepository
import com.example.pokeexplorer.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    uiState: TeamDetailUiState,
    onEvent: (TeamDetailEvent) -> Unit,
    onPokemonClick: (TeamMember) -> Unit,
    onBack: () -> Unit,
    onGoToList: () -> Unit,
) {
    // Храним id (Int), а не TeamMember: rememberSaveable не умеет сохранять data class.
    var editNoteFor by rememberSaveable { mutableStateOf<Int?>(null) }

    // Команду удалили (или сменился профиль): показывать нечего, закрываемся.
    LaunchedEffect(uiState.isLoading, uiState.teamExists) {
        if (!uiState.isLoading && !uiState.teamExists) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.teamName.ifBlank { "Команда" })
                        Text(
                            "${uiState.members.size}/${TeamRepository.MAX_TEAM_SIZE} покемонов",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                uiState.members.isEmpty() -> EmptyState(
                    title = "В команде пока никого",
                    subtitle = "Откройте покемона и нажмите «В команду»",
                    actionLabel = "К списку покемонов",
                    onAction = onGoToList,
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("team_members"),
                ) {
                    items(uiState.members, key = { it.pokemonId }) { member ->
                        MemberRow(
                            member = member,
                            onClick = { onPokemonClick(member) },
                            onEditNote = { editNoteFor = member.pokemonId },
                            onRemove = { onEvent(TeamDetailEvent.OnRemove(member.pokemonId)) },
                        )
                    }
                }
            }
        }
    }

    uiState.members.firstOrNull { it.pokemonId == editNoteFor }?.let { member ->
        NoteDialog(
            pokemonName = member.name.replaceFirstChar { it.uppercase() },
            initialNote = member.note,
            onConfirm = { note ->
                editNoteFor = null
                onEvent(TeamDetailEvent.OnEditNote(member.pokemonId, note))
            },
            onDismiss = { editNoteFor = null },
        )
    }
}

@Composable
private fun MemberRow(
    member: TeamMember,
    onClick: () -> Unit,
    onEditNote: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("member_${member.pokemonId}"),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${member.position}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = member.imageUrl,
                contentDescription = member.name,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    member.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    member.note.ifBlank { "Без заметки" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            IconButton(onClick = onEditNote) {
                Icon(Icons.Default.Edit, contentDescription = "Изменить заметку")
            }
            IconButton(onClick = onRemove, modifier = Modifier.testTag("remove_${member.pokemonId}")) {
                Icon(Icons.Default.Close, contentDescription = "Убрать из команды")
            }
        }
    }
}

@Composable
private fun NoteDialog(
    pokemonName: String,
    initialNote: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Заметка: $pokemonName") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Например: «ловец», «танк»") },
                maxLines = 3,
                modifier = Modifier.testTag("note_input"),
            )
        },
        // Пустая заметка допустима: так её можно стереть.
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}