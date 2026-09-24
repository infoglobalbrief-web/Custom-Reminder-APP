package com.remindly.app.features.people

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.remindly.app.core.domain.model.Person
import com.remindly.app.ui.components.BirthdayCard
import com.remindly.app.ui.components.EmptyState
import com.remindly.app.ui.components.PillChip
import com.remindly.app.ui.components.PillButton
import com.remindly.app.ui.components.PersonCard
import com.remindly.app.ui.components.SectionHeader
import com.remindly.app.ui.components.SolidCard
import com.remindly.app.ui.theme.AppRadius
import com.remindly.app.ui.theme.Danger
import com.remindly.app.ui.theme.PurplePrimary
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.width

/**
 * People / Birthdays — PRD §25–26:
 * birthday cards with countdown, add-birthday screen with REMIND ME rules.
 */
@Composable
fun PeopleScreen(
    container: AppContainer,
    padding: PaddingValues,
    onAddPerson: () -> Unit,
    onPersonClick: (Long) -> Unit,
) {
    var people by remember { mutableStateOf(listOf<Person>()) }
    var selectedBirthday by remember { mutableStateOf<Person?>(null) }

    LaunchedEffect(Unit) {
        container.peopleRepository.observePeople().collect { people = it }
    }

    val withBirthdays = people.filter { it.birthDate != null }
    val featured = selectedBirthday
        ?: withBirthdays.minByOrNull { it.daysUntilBirthday() ?: Int.MAX_VALUE }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("People", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            item { SectionHeader("BIRTHDAYS") }
            if (featured != null) {
                item {
                    BirthdayCard(
                        person = featured,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onPersonClick(featured.id) },
                    )
                }
                if (withBirthdays.size > 1) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            withBirthdays.take(4).forEach { p ->
                                PillChip(
                                    p.name,
                                    selected = p.id == featured.id,
                                    onClick = { selectedBirthday = p },
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    EmptyState(
                        title = stringResource(R.string.empty_birthday_title),
                        body = stringResource(R.string.empty_birthday_body),
                        actionText = stringResource(R.string.add_birthday),
                        onAction = onAddPerson,
                    )
                }
            }

            if (people.isNotEmpty()) {
                item { SectionHeader("ALL PEOPLE") }
                items(people, key = { it.id }) { person ->
                    PersonCard(person = person, onClick = { onPersonClick(person.id) })
                }
            }
        }
    }
}

/** Add Birthday / Person — PRD §26 full REMIND ME rules. */
@Composable
fun AddPersonScreen(
    container: AppContainer,
    personId: Long?,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var existing by remember { mutableStateOf<Person?>(null) }
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Father") }
    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    var startDays by remember { mutableStateOf(5) }
    var time by remember { mutableStateOf(LocalTime.of(22, 0)) }
    var repeatDaily by remember { mutableStateOf(true) }
    var birthdayDayAlert by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(personId) {
        if (personId != null && personId > 0) {
            container.peopleRepository.get(personId)?.let { p ->
                existing = p
                name = p.name
                relationship = p.relationship
                birthDate = p.birthDate
                startDays = p.reminderStartDays
                time = p.reminderTime
                birthdayDayAlert = p.birthdayDayAlert
            }
        }
    }

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
                stringResource(R.string.add_birthday),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(8.dp))

        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // Avatar / Add photo (PRD §26)
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .height(72.dp)
                                .width(72.dp)
                                .background(
                                    PurplePrimary.copy(alpha = 0.12f),
                                    RoundedCornerShape(999.dp)
                                ),
                            contentAlignment = Alignment.Center,
                        ) { Text("○", style = MaterialTheme.typography.headlineMedium, color = PurplePrimary) }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.add_photo),
                            style = MaterialTheme.typography.labelMedium,
                            color = PurplePrimary,
                        )
                    }
                }

                LabeledField(stringResource(R.string.name_label)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(AppRadius.sm),
                        colors = fieldColors(),
                    )
                }
                Spacer(Modifier.height(10.dp))
                LabeledField(stringResource(R.string.relationship)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Father", "Mother", "Brother", "Sister", "Friend", "Partner").forEach { rel ->
                            PillChip(rel, selected = relationship == rel, onClick = { relationship = rel })
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                LabeledField(stringResource(R.string.birthday_date)) {
                    SolidCard(
                        modifier = Modifier.fillMaxWidth(),
                        radius = AppRadius.sm,
                        onClick = {
                            val cal = java.util.Calendar.getInstance()
                            birthDate?.let {
                                cal.set(it.year, it.monthValue - 1, it.dayOfMonth)
                            }
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> birthDate = LocalDate.of(y, m + 1, d) },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH),
                            ).show()
                        },
                    ) {
                        Text(
                            birthDate?.format(DateTimeFormatter.ofPattern("d MMMM yyyy")) ?: "Select date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        // REMIND ME rules (PRD §26)
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SectionHeader(stringResource(R.string.remind_me))

                LabeledField(stringResource(R.string.remind_start)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 3, 5, 7, 10).forEach { d ->
                            PillChip("$d d", selected = startDays == d, onClick = { startDays = d })
                        }
                    }
                    Text(
                        stringResource(R.string.days_before, startDays),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))

                LabeledField(stringResource(R.string.reminder_time)) {
                    SolidCard(
                        modifier = Modifier.fillMaxWidth(),
                        radius = AppRadius.sm,
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m -> time = LocalTime.of(h, m) },
                                time.hour, time.minute, false,
                            ).show()
                        },
                    ) {
                        Text(
                            time.format(DateTimeFormatter.ofPattern("h:mm a")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                LabeledField(stringResource(R.string.repeat_label)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillChip(stringResource(R.string.repeat_daily), selected = repeatDaily, onClick = { repeatDaily = true })
                        PillChip("Once", selected = !repeatDaily, onClick = { repeatDaily = false })
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.birthday_day_notification),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    androidx.compose.material3.Switch(
                        checked = birthdayDayAlert,
                        onCheckedChange = { birthdayDayAlert = it },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                            checkedTrackColor = PurplePrimary,
                        ),
                    )
                }
            }
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        PillButton(
            text = stringResource(R.string.save_birthday),
            modifier = Modifier.fillMaxWidth(),
            gradient = true,
            onClick = {
                if (name.isBlank()) { error = "Please enter a name."; return@PillButton }
                if (birthDate == null) { error = "Please select a birthday."; return@PillButton }
                val person = Person(
                    id = existing?.id ?: 0L,
                    name = name.trim(),
                    relationship = relationship,
                    birthDate = birthDate,
                    birthdayEnabled = true,
                    reminderStartDays = startDays,
                    reminderTime = time,
                    birthdayDayAlert = birthdayDayAlert,
                    createdAt = existing?.createdAt ?: 0L,
                )
                kotlinx.coroutines.runBlocking { container.peopleRepository.save(person) }
                onDone()
            },
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
    content()
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PurplePrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = PurplePrimary,
)

/** Person / Birthday detail — PRD §25 View Reminder + countdown. */
@Composable
fun PersonDetailScreen(
    container: AppContainer,
    personId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    var person by remember { mutableStateOf<Person?>(null) }
    LaunchedEffect(personId) { person = container.peopleRepository.get(personId) }

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
            Text(stringResource(R.string.people), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(12.dp))

        val p = person
        if (p != null) {
            BirthdayCard(person = p, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionHeader("PROFILE")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Name", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text(p.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Relationship", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text(p.relationship.ifBlank { "—" }, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Birthday", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            p.birthDate?.format(DateTimeFormatter.ofPattern("d MMMM yyyy")) ?: "—",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    SectionHeader("REMINDERS")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Start", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.days_before, p.reminderStartDays),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Time", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            p.reminderTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            PillButton(
                text = stringResource(R.string.view_reminder),
                modifier = Modifier.fillMaxWidth(),
                gradient = true,
                onClick = { onEdit(personId) },
            )
        } else {
            Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(32.dp))
    }
}
