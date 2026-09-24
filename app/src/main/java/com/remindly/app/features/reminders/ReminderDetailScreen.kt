package com.remindly.app.features.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remindly.app.R
import com.remindly.app.core.di.AppContainer
import com.remindly.app.core.domain.model.Reminder
import com.remindly.app.core.domain.model.ReminderStatus
import kotlinx.coroutines.flow.first
import com.remindly.app.ui.components.GlassCard
import com.remindly.app.ui.components.PillButton
import com.remindly.app.ui.components.SectionHeader
import com.remindly.app.ui.components.SolidCard
import com.remindly.app.ui.theme.AppRadius
import com.remindly.app.ui.theme.Danger
import com.remindly.app.ui.theme.PurplePrimary
import java.time.format.DateTimeFormatter

/**
 * Reminder detail — PRD §32:
 * WHEN · REPEAT · UNTIL · Sound · Vibration · Snooze · Edit · Delete.
 */
@Composable
fun ReminderDetailScreen(
    container: AppContainer,
    reminderId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
) {
    var reminder by remember { mutableStateOf<Reminder?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(reminderId) {
        reminder = container.reminderRepository.get(reminderId)
    }

    val r = reminder
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                stringResource(R.string.reminder_detail),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onEdit(reminderId) }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = PurplePrimary)
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Danger)
            }
        }

        if (r == null) {
            Text(
                "Loading…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 40.dp),
            )
            return
        }

        Spacer(Modifier.height(8.dp))
        // Hero
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(r.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                if (r.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        r.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    r.startAt.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")),
                    style = MaterialTheme.typography.labelMedium,
                    color = PurplePrimary,
                )
                Text(
                    r.startAt.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SectionHeader("WHEN")
                DetailRow(
                    stringResource(R.string.start_time),
                    r.startAt.format(DateTimeFormatter.ofPattern("h:mm a")),
                )
                DetailRow("Date", r.startAt.format(DateTimeFormatter.ofPattern("d MMM yyyy")))
                SectionHeader("REPEAT")
                DetailRow(
                    "Repeat",
                    when (r.rule) {
                        com.remindly.app.core.domain.model.RepeatRule.NONE -> stringResource(R.string.repeat_never)
                        else -> r.rule.serialize().substringBefore(";UNTIL=").replace(";", " · ")
                    },
                )
                DetailRow(
                    "Until",
                    r.rule.until?.format(DateTimeFormatter.ofPattern("d MMM yyyy")) ?: stringResource(R.string.no_end_date),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SectionHeader("ALARM")
                DetailRow(stringResource(R.string.sound), if (r.soundEnabled) "Default" else "Off")
                DetailRow(stringResource(R.string.vibration), if (r.vibrationEnabled) "On" else "Off")
                DetailRow(stringResource(R.string.snooze_label), if (r.snoozeEnabled) "${r.snoozeMinutes} minutes" else "Off")
            }
        }

        Spacer(Modifier.height(14.dp))
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SectionHeader("ORGANIZE")
                DetailRow(stringResource(R.string.category), r.category)
                DetailRow(stringResource(R.string.priority), r.priority.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }

        Spacer(Modifier.height(20.dp))
        PillButton(
            text = stringResource(R.string.edit_reminder),
            modifier = Modifier.fillMaxWidth(),
            gradient = true,
            onClick = { onEdit(reminderId) },
        )

        if (r.status == ReminderStatus.ACTIVE && r.rule.freq == com.remindly.app.core.domain.model.Freq.NONE) {
            Spacer(Modifier.height(10.dp))
            PillButton(
                text = stringResource(R.string.action_done),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    kotlinx.coroutines.runBlocking {
                        container.reminderRepository.markCompleted(reminderId, r.startAt)
                    }
                    onDeleted() // pop back; card shows completed
                },
            )
        }

        if (confirmDelete) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(stringResource(R.string.delete_reminder)) },
                text = { Text(stringResource(R.string.delete_confirm_body)) },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        kotlinx.coroutines.runBlocking { container.reminderRepository.delete(reminderId) }
                        container.scheduler.cancelReminder(reminderId)
                        confirmDelete = false
                        onDeleted()
                    }) { Text(stringResource(R.string.action_delete), color = Danger) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                shape = RoundedCornerShape(24.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Search screen — PRD §37 search/filter. */
@Composable
fun SearchScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onReminderClick: (Long) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<Reminder>()) }

    androidx.compose.runtime.LaunchedEffect(query) {
        results = if (query.isBlank()) emptyList()
        else container.reminderRepository.search(query).first()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                ),
            )
        }
        Spacer(Modifier.height(12.dp))
        results.forEach { r ->
            SolidCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                onClick = { onReminderClick(r.id) },
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(r.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        r.startAt.format(DateTimeFormatter.ofPattern("d MMM yyyy · h:mm a")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (query.isNotBlank() && results.isEmpty()) {
            com.remindly.app.ui.components.EmptyState(
                title = "No matches",
                body = "Try a different search term.",
            )
        }
    }
}
