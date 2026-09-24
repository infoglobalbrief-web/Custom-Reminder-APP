package com.remindly.app.features.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.remindly.app.R
import com.remindly.app.core.di.AppContainer
import com.remindly.app.core.domain.model.AlertSpec
import com.remindly.app.core.domain.model.Priority
import com.remindly.app.core.domain.model.Reminder
import com.remindly.app.core.domain.model.RepeatRule
import com.remindly.app.ui.components.PillChip
import com.remindly.app.ui.components.PillButton
import com.remindly.app.ui.theme.AppRadius
import com.remindly.app.ui.theme.PurplePrimary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.TimeZone
import androidx.compose.foundation.clickable

/**
 * Quick-add bottom sheet — PRD §22:
 * New Reminder · Today/Tomorrow pills · time pill · More options ›
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    container: AppContainer,
    onDismiss: () -> Unit,
    onMoreOptions: () -> Unit,
    onCreated: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var dayChoice by remember { mutableStateOf(0) } // 0=today, 1=tomorrow
    var time by remember { mutableStateOf(LocalTime.of(20, 0)) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(999.dp),
                    )
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        ) {
            Text(
                stringResource(R.string.new_reminder),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.what_to_remember),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.quick_title_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("Today", selected = dayChoice == 0, onClick = { dayChoice = 0 })
                PillChip("Tomorrow", selected = dayChoice == 1, onClick = { dayChoice = 1 })
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TimePickerPill(
                    label = time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")),
                    onPicked = { time = it },
                )
                // Type pill (static "Reminder" — advanced types in More options)
                PillChip("Reminder", selected = true)
            }

            Spacer(Modifier.height(16.dp))
            // More options › → Advanced Reminder screen (PRD §23)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .androidxClickableNoRipple { onMoreOptions() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.more_options),
                    style = MaterialTheme.typography.labelLarge,
                    color = PurplePrimary,
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PurplePrimary,
                )
            }

            Spacer(Modifier.height(14.dp))
            PillButton(
                text = stringResource(R.string.create_reminder),
                modifier = Modifier.fillMaxWidth(),
                gradient = true,
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isEmpty()) {
                        error = "Please enter a title."
                        return@PillButton
                    }
                    val date = LocalDate.now().plusDays(dayChoice.toLong())
                    val reminder = Reminder(
                        title = trimmed,
                        startAt = LocalDateTime.of(date, time),
                        timezone = TimeZone.getDefault().id,
                        rule = RepeatRule.NONE,
                        priority = Priority.NORMAL,
                        alerts = listOf(AlertSpec()),
                    )
                    kotlinx.coroutines.runBlocking {
                        val id = container.reminderRepository.save(reminder)
                        val saved = container.reminderRepository.get(id)
                        if (saved != null) container.scheduler.schedule(saved)
                    }
                    onCreated()
                },
            )
        }
    }
}

private fun Modifier.androidxClickableNoRipple(onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )
