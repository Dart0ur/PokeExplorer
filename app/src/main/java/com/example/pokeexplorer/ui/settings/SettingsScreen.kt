package com.example.pokeexplorer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.pokeexplorer.data.sync.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Настройки относятся к текущему профилю",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ------------------------------------------------ Кэш
            SectionTitle("Срок жизни кэша")
            SettingsViewModel.TTL_OPTIONS.forEach { hours ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = uiState.ttlHours == hours,
                            onClick = { onEvent(SettingsEvent.OnTtlSelected(hours)) },
                            role = Role.RadioButton,
                        )
                        .testTag("ttl_$hours")
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = uiState.ttlHours == hours, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(ttlLabel(hours))
                }
            }

            HorizontalDivider()

            // ------------------------------------------------ Переключатели
            SwitchRow(
                title = "Только Wi-Fi",
                subtitle = "Фоновое обновление не тратит мобильный трафик",
                checked = uiState.wifiOnly,
                onChange = { onEvent(SettingsEvent.OnWifiOnlyChanged(it)) },
                tag = "switch_wifi_only",
            )
            SwitchRow(
                title = "Показывать скрытых",
                subtitle = "Скрытые покемоны снова появятся в списке",
                checked = uiState.showHidden,
                onChange = { onEvent(SettingsEvent.OnShowHiddenChanged(it)) },
                tag = "switch_show_hidden",
            )

            HorizontalDivider()

            // ------------------------------------------------ Синхронизация
            SectionTitle("Офлайн-данные")
            Text(
                text = uiState.lastSyncAt?.let { "Последнее обновление: ${formatTime(it)}" }
                    ?: "Фоновое обновление ещё не выполнялось",
                modifier = Modifier.testTag("last_sync"),
            )
            Text(
                "Избранное, команды и история обновляются автоматически раз в 12 часов " +
                        "и доступны без сети",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { onEvent(SettingsEvent.OnSyncNow) },
                enabled = uiState.syncStatus == SyncStatus.IDLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_now"),
            ) {
                if (uiState.syncStatus == SyncStatus.RUNNING) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    when (uiState.syncStatus) {
                        SyncStatus.IDLE -> "Обновить сейчас"
                        SyncStatus.RUNNING -> "Обновляем…"
                        SyncStatus.WAITING_FOR_NETWORK ->
                            if (uiState.wifiOnly) "Ждём Wi-Fi…" else "Ждём сеть…"
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onChange, role = Role.Switch)
            .testTag(tag)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun ttlLabel(hours: Int): String = when (hours) {
    0 -> "Всегда загружать заново"
    1 -> "1 час"
    in 2..4 -> "$hours часа"
    24 -> "1 день"
    72 -> "3 дня"
    else -> "$hours часов"
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(millis))