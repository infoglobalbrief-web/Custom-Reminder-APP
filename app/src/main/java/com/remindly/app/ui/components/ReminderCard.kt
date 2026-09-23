package com.remindly.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindly.app.core.domain.model.Priority
import com.remindly.app.ui.theme.AccentGreen
import com.remindly.app.ui.theme.AppRadius
import com.remindly.app.ui.theme.PriorityHigh
import com.remindly.app.ui.theme.PriorityLow
import com.remindly.app.ui.theme.PurplePrimary
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

/**
 * Compact task/reminder card with swipe gestures (PRD §21, §31):
 *   swipe right → Complete · swipe left → Delete · tap → Details
 */
@Composable
fun ReminderCard(
    title: String,
    subtitle: String?,
    time: String,
    category: String,
    completed: Boolean,
    priority: Priority,
    modifier: Modifier = Modifier,
    isNext: Boolean = false,
    onTap: () -> Unit = {},
    onComplete: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
                SwipeToDismissBoxValue.StartToEnd -> { onComplete(); false }
                SwipeToDismissBoxValue.Settled -> true
            }
        },
        positionalThreshold = { total -> total * 0.35f },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,   // swipe right → complete
        enableDismissFromEndToStart = true,   // swipe left → delete
        backgroundContent = { DismissBackground() },
        modifier = modifier.fillMaxWidth(),
    ) {
        SolidCard(
            modifier = Modifier.fillMaxWidth(),
            radius = AppRadius.card,
            onClick = onTap,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Micro-skeuomorphic check circle (PRD §7)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (completed) AccentGreen
                            else when (priority) {
                                Priority.HIGH -> PriorityHigh.copy(alpha = 0.12f)
                                Priority.LOW -> PriorityLow.copy(alpha = 0.12f)
                                Priority.NORMAL -> PurplePrimary.copy(alpha = 0.10f)
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (completed) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = when (priority) {
                                Priority.HIGH -> PriorityHigh
                                Priority.LOW -> PriorityLow
                                Priority.NORMAL -> PurplePrimary
                            },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (completed) TextDecoration.LineThrough else null,
                        color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            time,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isNext) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CategoryTag(category)
                        if (isNext) {
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = "Next reminder",
                                tint = PurplePrimary,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTag(category: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(PurplePrimary.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(category, style = MaterialTheme.typography.labelSmall, color = PurplePrimary)
    }
}

@Composable
private fun DismissBackground() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(AppRadius.card))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        AccentGreen.copy(alpha = 0.35f),
                        Color.Transparent,
                        Color.Transparent,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.30f),
                    )
                )
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓ Done", style = MaterialTheme.typography.labelLarge, color = AccentGreen.copy(alpha = 0.9f))
        Text("Delete", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
    }
}

// ---------------------------------------------------------------------------
// Progress card — PRD §18 TODAY x/y completed with progress bar
// ---------------------------------------------------------------------------
@Composable
fun ProgressCard(
    done: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    val percent = (progress * 100).toInt()
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("TODAY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(
                "$done / $total completed",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.horizontalGradient(listOf(PurplePrimary, Color(0xFFA77AF3)))),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "$percent%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PurplePrimary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Section header — PRD §60
// ---------------------------------------------------------------------------
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(vertical = 6.dp),
    )
}

// ---------------------------------------------------------------------------
// Empty state — PRD §63
// ---------------------------------------------------------------------------
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✦", style = MaterialTheme.typography.displaySmall, color = PurplePrimary)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            PillButton(text = actionText, gradient = true, onClick = onAction)
        }
    }
}
