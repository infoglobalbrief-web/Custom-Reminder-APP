package com.remindly.app.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remindly.app.core.di.AppContainer
import com.remindly.app.core.domain.model.Reminder
import com.remindly.app.ui.components.GlassCard
import com.remindly.app.ui.components.ReminderCard
import com.remindly.app.ui.components.SectionHeader
import com.remindly.app.ui.components.SegmentedPills
import com.remindly.app.ui.theme.AppRadius
import com.remindly.app.ui.theme.PurplePrimary
import com.remindly.app.ui.theme.PurpleSecondary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

/**
 * Calendar — PRD §29 modern floating calendar + day agenda.
 * Segmented [ Month ] [ Week ] [ Day ] [ Agenda ] per PRD §30/§60.
 */
@Composable
fun CalendarScreen(
    container: AppContainer,
    padding: PaddingValues,
    onReminderClick: (Long) -> Unit,
) {
    var viewMode by remember { mutableStateOf(0) } // 0 month, 1 week, 2 day, 3 agenda
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var dayItems by remember { mutableStateOf(listOf<Pair<Reminder, LocalDateTime>>()) }
    var markedDays by remember { mutableStateOf(setOf<LocalDate>()) }

    suspend fun reload() {
        dayItems = container.reminderRepository.occurrencesOn(selected)
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        val marks = HashSet<LocalDate>()
        var cursor = first
        while (!cursor.isAfter(last)) {
            if (container.reminderRepository.occurrencesOn(cursor).isNotEmpty()) marks += cursor
            cursor = cursor.plusDays(1)
        }
        markedDays = marks
    }

    LaunchedEffect(selected, month) { reload() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                start = 20.dp, end = 20.dp,
                top = padding.calculateTopPadding() + 12.dp,
            ),
    ) {
        Text("Calendar", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        SegmentedPills(
            options = listOf("Month", "Week", "Day", "Agenda"),
            selectedIndex = viewMode,
            onSelect = { viewMode = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        // Floating month header (PRD §29)
        GlassCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.xl) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1); markedDays = emptySet() }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous", tint = PurplePrimary)
                    }
                    Text(
                        month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = { month = month.plusMonths(1); markedDays = emptySet() }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next", tint = PurplePrimary)
                    }
                }

                // Weekday header
                val weekFields = WeekFields.of(java.time.DayOfWeek.MONDAY, 4)
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekFields.dayOfWeek.minimalDaysInFirstWeek
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                        Text(
                            d,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                val firstDay = month.atDay(1)
                val lead = (firstDay.dayOfWeek.value + 6) % 7 // Monday-first
                val totalDays = month.lengthOfMonth()
                val cells = mutableListOf<LocalDate?>()
                repeat(lead) { cells += null }
                for (d in 1..totalDays) cells += month.atDay(d)
                while (cells.size % 7 != 0) cells += null

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    cells.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                Box(modifier = Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                                    if (date != null) CalendarCell(
                                        date = date,
                                        selected = date == selected,
                                        isToday = date == LocalDate.now(),
                                        hasItems = date in markedDays,
                                        onClick = { selected = date },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        LazyColumn(
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionHeader(
                    if (selected == LocalDate.now()) "TODAY"
                    else selected.format(DateTimeFormatter.ofPattern("d MMMM")).uppercase()
                )
            }
            if (dayItems.isEmpty()) {
                item {
                    com.remindly.app.ui.components.EmptyState(
                        title = "Nothing planned",
                        body = "No reminders on this day.",
                    )
                }
            } else {
                items(dayItems.size) { i ->
                    val (reminder, at) = dayItems[i]
                    ReminderCard(
                        title = reminder.title,
                        subtitle = reminder.description.ifBlank { null },
                        time = at.format(DateTimeFormatter.ofPattern("h:mm a")),
                        category = reminder.category,
                        completed = false,
                        priority = reminder.priority,
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
        }
    }
}

/** Calendar cell — selected date appears physically raised (PRD §7). */
@Composable
private fun CalendarCell(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    hasItems: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(38.dp)
            .then(
                if (selected) Modifier.shadow(8.dp, shape, ambientColor = PurplePrimary, spotColor = PurplePrimary)
                else Modifier
            )
            .clip(shape)
            .background(
                when {
                    selected -> Brush.verticalGradient(listOf(PurplePrimary, PurplePrimary.copy(alpha = 0.85f)))
                    isToday -> Brush.verticalGradient(listOf(PurpleSecondary.copy(alpha = 0.25f), PurpleSecondary.copy(alpha = 0.15f)))
                    else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    selected -> Color.White
                    isToday -> PurplePrimary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            if (hasItems) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else PurplePrimary),
                )
            }
        }
    }
}
