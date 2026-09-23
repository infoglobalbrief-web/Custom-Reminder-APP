package com.remindly.app.features.reminders

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.remindly.app.R
import com.remindly.app.core.di.AppContainer
import com.remindly.app.core.domain.model.AlertSpec
import com.remindly.app.core.domain.model.Freq
import com.remindly.app.core.domain.model.IntervalUnit
import com.remindly.app.core.domain.model.Priority
import com.remindly.app.core.domain.model.Reminder
import com.remindly.app.core.domain.model.ReminderStatus
import com.remindly.app.core.domain.model.RepeatRule
import com.remindly.app.ui.components.PillChip
import com.remindly.app.ui.components.PillButton
import com.remindly.app.ui.components.SectionHeader
import com.remindly.app.ui.components.SolidCard
import com.remindly.app.ui.theme.AppRadius
import com.remindly.app.ui.theme.PurplePrimary
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * Advanced create/edit reminder — PRD §23:
 * Title · Description · WHEN · REPEAT · ALERTS · ALARM · ORGANIZE.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditReminderScreen(
    container: AppContainer,
    reminderId: Long?,
    presetDate: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    var existing by remember { mutableStateOf<Reminder?>(null) }
    var loaded by remember { mutableStateOf(reminderId == null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.of(20, 0)) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var freq by remember { mutableStateOf(Freq.NONE) }
    var interval by remember { mutableStateOf(1) }
    var unit by remember { mutableStateOf(IntervalUnit.DAYS) }
    var until by remember { mutableStateOf<LocalDate?>(null) }
    var alertOffsets by remember { mutableStateOf(listOf(0)) } // minutes before
    var sound by remember { mutableStateOf(true) }
    var vibration by remember { mutableStateOf(true) }
    var snoozeEnabled by remember { mutableStateOf(true) }
    var snoozeMinutes by remember { mutableStateOf(10) }
    var category by remember { mutableStateOf("Personal") }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var showDatePick by remember { mutableStateOf(false) }
    var showTimePick by remember { mutableStateOf(false) }
    var showEndPick by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reminderId) {
        if (reminderId != null && reminderId > 0) {
            container.reminderRepository.get(reminderId)?.let { r ->
                existing = r
                title = r.title
                description = r.description
                startDate = r.startAt.toLocalDate()
                startTime = r.startAt.toLocalTime()
                endDate = r.endAt?.toLocalDate()
                freq = r.rule.freq
                interval = r.rule.interval
                unit = r.rule.unit
                until = r.rule.until
                alertOffsets = r.alerts.map { it.offsetMinutes }.ifEmpty { listOf(0) }
                sound = r.soundEnabled
                vibration = r.vibrationEnabled
                snoozeEnabled = r.snoozeEnabled
                snoozeMinutes = r.snoozeMinutes
                category = r.category
                priority = r.priority
            }
        }
        presetDate?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()?.let { startDate = it }
        }
        loaded = true
    }

    if (!loaded) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // Top bar
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                stringResource(if (reminderId == null || reminderId <= 0) R.string.create_reminder_title else R.string.edit_reminder_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(8.dp))

        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                FieldLabel(stringResource(R.string.field_title))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(AppRadius.sm),
                    colors = outlinedColors(),
                )
                Spacer(Modifier.height(10.dp))
                FieldLabel(stringResource(R.string.field_description))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().height(88.dp),
                    shape = RoundedCornerShape(AppRadius.sm),
                    colors = outlinedColors(),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        // WHEN (PRD §23)
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SectionHeader(stringResource(R.string.section_when))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditablePill(
                        label = stringResource(R.string.start_date),
                        value = startDate.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                        onClick = { showDatePick = true },
                        modifier = Modifier.weight(1f),
                    )
                    EditablePill(
                        label = stringResource(R.string.start_time),
                        value = startTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                        onClick = { showTimePick = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                EditablePill(
                    label = stringResource(R.string.end_date),
                    value = endDate?.format(DateTimeFormatter.ofPattern("d MMM yyyy")) ?: stringResource(R.string.no_end_date),
                    onClick = { showEndPick = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        // REPEAT (PRD §23, §37)
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SectionHeader(stringResource(R.string.section_repeat))
                val options = listOf(
                    Freq.NONE to stringResource(R.string.repeat_never),
                    Freq.DAILY to stringResource(R.string.repeat_daily),
                    Freq.WEEKDAYS to stringResource(R.string.repeat_weekdays),
                    Freq.WEEKLY to stringResource(R.string.repeat_weekly),
                    Freq.MONTHLY to stringResource(R.string.repeat_monthly),
                    Freq.YEARLY to stringResource(R.string.repeat_yearly),
                    Freq.CUSTOM to stringResource(R.string.repeat_custom),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    options.take(4).forEach { (f, label) ->
                        PillChip(label, selected = freq == f, onClick = { freq = f })
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.drop(4).forEach { (f, label) ->
                        PillChip(label, selected = freq == f, onClick = { freq = f })
                    }
                }
                if (freq == Freq.CUSTOM) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Every", style = MaterialTheme.typography.bodyMedium)
                        PillChip("$interval", selected = true, onClick = {
                            interval = (interval % 30) + 1
                        })
                        listOf(IntervalUnit.DAYS to "Days", IntervalUnit.WEEKS to "Weeks", IntervalUnit.MONTHS to "Months").forEach { (u, label) ->
                            PillChip(label, selected = unit == u, onClick = { unit = u })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                EditablePill(
                    label = "Until (optional)",
                    value = until?.format(DateTimeFormatter.ofPattern("d MMM yyyy")) ?: "Never-ending",
                    onClick = { until = if (until == null) startDate.plusMonths(3) else null },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        // ALERTS — multiple alerts (PRD §24)
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SectionHeader(stringResource(R.string.section_alerts))
                alertOffsets.forEachIndexed { index, offset ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Text(
                            if (offset == 0) "At time" else "$offset min before",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (alertOffsets.size > 1) {
                            TextButton(onClick = { alertOffsets = alertOffsets.filterIndexed { i, _ -> i != index } }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                TextButton(onClick = {
                    alertOffsets = alertOffsets + listOf(5, 10, 15, 30, 60, 1440).firstOrNull { it !in alertOffsets }!!
                }) {
                    Text("+ ${stringResource(R.string.add_alert)}", color = PurplePrimary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        // ALARM (PRD §23)
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SectionHeader(stringResource(R.string.section_alarm))
                ToggleRow(stringResource(R.string.sound), sound) { sound = it }
                ToggleRow(stringResource(R.string.vibration), vibration) { vibration = it }
                ToggleRow("${stringResource(R.string.snooze_label)} · ${snoozeMinutes} min", snoozeEnabled) { snoozeEnabled = it }
            }
        }

        Spacer(Modifier.height(14.dp))
        // ORGANIZE (PRD §23, §37)
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SectionHeader(stringResource(R.string.section_organize))
                Text(stringResource(R.string.category), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Personal", "Work", "Family", "Health").forEach { c ->
                        PillChip(c, selected = category == c, onClick = { category = c })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.priority), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Priority.entries.forEach { p ->
                        PillChip(p.name.lowercase().replaceFirstChar { it.uppercase() }, selected = priority == p, onClick = { priority = p })
                    }
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        PillButton(
            text = stringResource(R.string.save_reminder),
            modifier = Modifier.fillMaxWidth(),
            gradient = true,
            onClick = {
                if (title.isBlank()) {
                    error = "Please enter a title."
                    return@PillButton
                }
                val rule = RepeatRule(
                    freq = freq,
                    interval = interval,
                    unit = unit,
                    byDays = if (freq == Freq.WEEKLY) setOf(startDate.dayOfWeek) else emptySet(),
                    until = until,
                )
                val reminder = Reminder(
                    id = existing?.id ?: 0L,
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    startAt = LocalDateTime.of(startDate, startTime),
                    endAt = endDate?.atTime(LocalTime.of(23, 59)),
                    timezone = TimeZone.getDefault().id,
                    rule = rule,
                    status = existing?.status ?: ReminderStatus.ACTIVE,
                    soundEnabled = sound,
                    vibrationEnabled = vibration,
                    snoozeEnabled = snoozeEnabled,
                    snoozeMinutes = snoozeMinutes,
                    alerts = alertOffsets.map { AlertSpec(offsetMinutes = it) },
                    createdAt = existing?.createdAt ?: 0L,
                )
                kotlinx.coroutines.runBlocking {
                    val id = container.reminderRepository.save(reminder)
                    container.reminderRepository.get(id)?.let { container.scheduler.schedule(it) }
                }
                onDone()
            },
        )
        Spacer(Modifier.height(32.dp))
    }

    // ---- Date pickers (skeuomorphic raised calendar date §7) ----
    if (showDatePick || showEndPick) {
        val initial = (if (showDatePick) startDate else endDate ?: startDate)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showDatePick = false; showEndPick = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val d = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        if (showDatePick) startDate = d else endDate = d
                    }
                    showDatePick = false; showEndPick = false
                }) { Text("OK", color = PurplePrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePick = false; showEndPick = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = state) }
    }

    if (showTimePick) {
        val state = rememberTimePickerState(initialHour = startTime.hour, initialMinute = startTime.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePick = false },
            confirmButton = {
                TextButton(onClick = {
                    startTime = LocalTime.of(state.hour, state.minute)
                    showTimePick = false
                }) { Text("OK", color = PurplePrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePick = false }) { Text(stringResource(R.string.cancel)) }
            },
            text = { TimePicker(state = state) },
            shape = RoundedCornerShape(24.dp),
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun EditablePill(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SolidCard(modifier = modifier, radius = AppRadius.md, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = PurplePrimary,
            ),
        )
    }
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PurplePrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = PurplePrimary,
)

/** Time picker pill (reused by QuickAdd). */
@Composable
fun TimePickerPill(label: String, onPicked: (LocalTime) -> Unit) {
    val context = LocalContext.current
    PillButton(
        text = "🕘  $label",
        onClick = {
            TimePickerDialog(
                context,
                { _, h, m -> onPicked(LocalTime.of(h, m)) },
                20, 0, false,
            ).show()
        },
    )
}
