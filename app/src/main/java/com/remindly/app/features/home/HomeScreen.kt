package com.remindly.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remindly.app.core.di.AppContainer
import com.remindly.app.core.domain.model.Person
import com.remindly.app.core.domain.model.Priority
import com.remindly.app.core.domain.model.Reminder
import com.remindly.app.ui.components.BirthdayCard
import com.remindly.app.ui.components.DatePill
import com.remindly.app.ui.components.EmptyState
import com.remindly.app.ui.components.GlassCard
import com.remindly.app.ui.components.ProgressCard
import com.remindly.app.ui.components.ReminderCard
import com.remindly.app.ui.components.SectionHeader
import com.remindly.app.ui.theme.PurplePrimary
import com.remindly.app.ui.theme.PurpleSecondary
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Home dashboard — PRD §18–21, §27 (signature screen):
 * greeting · progress · NEXT · today's compact cards · birthday countdown.
 */
@Composable
fun HomeScreen(
    container: AppContainer,
    userName: String,
    padding: PaddingValues,
    onReminderClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var today by remember { mutableStateOf(LocalDateTime.now().toLocalDate()) }
    var progress by remember { mutableStateOf(0 to 0) }
    var dayOccurrences by remember { mutableStateOf(listOf<Pair<Reminder, LocalDateTime>>()) }
    var next by remember { mutableStateOf<Pair<Reminder, LocalDateTime>?>(null) }
    var birthdayPerson by remember { mutableStateOf<Person?>(null) }
    var completions by remember { mutableStateOf(setOf<Pair<Long, Long>>()) }

    suspend fun reload() {
        today = LocalDate.now()
        progress = container.reminderRepository.todayProgress(selectedDate)
        dayOccurrences = container.reminderRepository.occurrencesOn(selectedDate)
        completions = container.reminderRepository.completionSet()
        next = container.reminderRepository.nextOccurrence()?.let { occ ->
            container.reminderRepository.get(occ.reminderId)?.let { it to occ.at }
        }
        birthdayPerson = container.peopleRepository.getAll()
            .filter { it.birthDate != null && it.birthdayEnabled }
            .minByOrNull { it.daysUntilBirthday() ?: Int.MAX_VALUE }
    }

    LaunchedEffect(selectedDate) { reload() }

    val hour = LocalDateTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val displayName = userName.ifBlank { "there" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        PurplePrimary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background,
                    )
                )
            ),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---- Greeting header (PRD §18) ----
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "$greeting, $displayName",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        selectedDate.format(DateTimeFormatter.ofPattern("EEEE · d MMMM")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = PurplePrimary)
                }
            }
        }

        // ---- Progress card (PRD §18) ----
        item {
            ProgressCard(done = progress.first, total = progress.second, modifier = Modifier.fillMaxWidth())
        }

        // ---- Horizontal date selector (PRD §20) ----
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val start = selectedDate.minusDays(3)
                items(8) { i ->
                    val date = start.plusDays(i.toLong())
                    DatePill(
                        dayOfMonth = date.dayOfMonth.toString(),
                        dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ROOT),
                        selected = date == selectedDate,
                        onClick = { selectedDate = date },
                    )
                }
            }
        }

        // ---- NEXT reminder glass card (PRD §18) ----
        item {
            SectionHeader("NEXT")
            val nextPair = next
            if (nextPair != null) {
                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onReminderClick(nextPair.first.id) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            nextPair.second.format(DateTimeFormatter.ofPattern("h:mm a")),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(nextPair.first.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (nextPair.second.toLocalDate() == LocalDate.now()) "Today"
                            else nextPair.second.format(DateTimeFormatter.ofPattern("d MMM")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("✨", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Nothing scheduled next",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ---- Today's task cards (PRD §21) ----
        item {
            SectionHeader("TODAY")
        }
        if (dayOccurrences.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing planned",
                    body = "Your day is clear.",
                    actionText = "+ Add Reminder",
                    onAction = onAddClick,
                )
            }
        } else {
            items(dayOccurrences, key = { "${it.first.id}-${it.second}" }) { (reminder, at) ->
                val key = reminder.id to at.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                ReminderCard(
                    title = reminder.title,
                    subtitle = reminder.description.ifBlank { null },
                    time = at.format(DateTimeFormatter.ofPattern("h:mm a")),
                    category = reminder.category,
                    completed = key in completions,
                    priority = reminder.priority,
                    isNext = next?.first?.id == reminder.id,
                    onTap = { onReminderClick(reminder.id) },
                    onComplete = {
                        kotlinx.coroutines.runBlocking {
                            container.reminderRepository.markCompleted(reminder.id, at)
                            reload()
                        }
                    },
                    onDelete = {
                        kotlinx.coroutines.runBlocking {
                            container.reminderRepository.delete(reminder.id)
                            reload()
                        }
                    },
                )
            }
        }

        // ---- Birthday countdown (PRD §27) ----
        birthdayPerson?.let { person ->
            item {
                SectionHeader("FAMILY")
                BirthdayCard(person = person, modifier = Modifier.fillMaxWidth())
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
