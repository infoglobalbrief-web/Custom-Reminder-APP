package com.remindly.app.features.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remindly.app.core.di.AppContainer
import com.remindly.app.core.domain.model.Reminder
import com.remindly.app.core.domain.recurrence.RecurrenceEngine
import com.remindly.app.ui.components.EmptyState
import com.remindly.app.ui.components.ReminderCard
import com.remindly.app.ui.components.SectionHeader
import com.remindly.app.ui.components.SegmentedPills
import com.remindly.app.ui.theme.PurplePrimary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.fillMaxWidth

/**
 * Tasks screen — PRD §30–31:
 * [ Today ] [ Upcoming ] [ Completed ] + Morning/Afternoon/Evening groups,
 * swipe gestures, tap → detail.
 */
@Composable
fun TasksScreen(
    container: AppContainer,
    padding: PaddingValues,
    onReminderClick: (Long) -> Unit,
    onAddClick: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var todayOcc by remember { mutableStateOf(listOf<Pair<Reminder, LocalDateTime>>()) }
    var upcoming by remember { mutableStateOf(listOf<Pair<Reminder, LocalDateTime>>()) }
    var completed by remember { mutableStateOf(listOf<Pair<Reminder, LocalDateTime>>()) }
    var completionKeys by remember { mutableStateOf(setOf<Pair<Long, Long>>()) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        completionKeys = container.reminderRepository.completionSet()
        todayOcc = container.reminderRepository.occurrencesOn(today)

        val all = container.reminderRepository.getAll()
            .filter { it.status.name == "ACTIVE" && it.deletedAt == null }
        val up = ArrayList<Pair<Reminder, LocalDateTime>>()
        for (r in all) {
            val from = today.plusDays(1).atStartOfDay()
            RecurrenceEngine.nextOccurrence(r.rule, r.startAt, from)?.let { up += r to it }
        }
        upcoming = up.sortedBy { it.second }.take(50)

        // Completed: known completion timestamps paired with their reminder
        val comps = ArrayList<Pair<Reminder, LocalDateTime>>()
        for (r in all) {
            for (occ in RecurrenceEngine.occurrencesBetween(r.rule, r.startAt, today.minusDays(30).atStartOfDay(), today.plusDays(1).atStartOfDay())) {
                val key = r.id to occ.atZone(zone).toInstant().toEpochMilli()
                if (key in completionKeys) comps += r to occ
            }
            if (r.status.name == "COMPLETED") comps += r to r.startAt
        }
        completed = comps.sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        PurplePrimary.copy(alpha = 0.04f),
                    )
                )
            )
            .padding(
                start = 20.dp, end = 20.dp,
                top = padding.calculateTopPadding() + 12.dp,
            ),
    ) {
        Text("Tasks", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))

        SegmentedPills(
            options = listOf("Today", "Upcoming", "Completed"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        val list = when (tab) {
            0 -> todayOcc
            1 -> upcoming
            else -> completed
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = padding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        title = "No tasks here",
                        body = "Tap + to create your first reminder.",
                        actionText = if (tab == 0) "+ Add Reminder" else null,
                        onAction = onAddClick,
                    )
                }
            } else if (tab == 0) {
                // PRD §30 time-of-day grouping
                val groups = listOf(
                    Triple("Morning", RecurrenceEngine.DayPart.MORNING, ArrayList<Pair<Reminder, LocalDateTime>>()),
                    Triple("Afternoon", RecurrenceEngine.DayPart.AFTERNOON, ArrayList<Pair<Reminder, LocalDateTime>>()),
                    Triple("Evening", RecurrenceEngine.DayPart.EVENING, ArrayList<Pair<Reminder, LocalDateTime>>()),
                ).map { (label, part, acc) ->
                    label to list.filter { RecurrenceEngine.dayPart(it.second.toLocalTime()) == part }
                }
                for ((label, items) in groups) {
                    if (items.isEmpty()) continue
                    item { SectionHeader(label.uppercase()) }
                    items(items, key = { "${it.first.id}-${it.second}" }) { (reminder, at) ->
                        TaskRow(container, reminder, at, completionKeys, onReminderClick, onRefresh = { refresh++ })
                    }
                }
            } else {
                items(list, key = { "${it.first.id}-${it.second}" }) { (reminder, at) ->
                    TaskRow(container, reminder, at, completionKeys, onReminderClick, onRefresh = { refresh++ })
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    container: AppContainer,
    reminder: Reminder,
    at: LocalDateTime,
    completionKeys: Set<Pair<Long, Long>>,
    onReminderClick: (Long) -> Unit,
    onRefresh: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val key = reminder.id to at.atZone(zone).toInstant().toEpochMilli()
    ReminderCard(
        title = reminder.title,
        subtitle = reminder.description.ifBlank { null },
        time = at.format(DateTimeFormatter.ofPattern("h:mm a")),
        category = reminder.category,
        completed = key in completionKeys || reminder.status.name == "COMPLETED",
        priority = reminder.priority,
        onTap = { onReminderClick(reminder.id) },
        onComplete = {
            kotlinx.coroutines.runBlocking {
                container.reminderRepository.markCompleted(reminder.id, at)
                onRefresh()
            }
        },
        onDelete = {
            kotlinx.coroutines.runBlocking {
                container.reminderRepository.delete(reminder.id)
                onRefresh()
            }
        },
    )
}

