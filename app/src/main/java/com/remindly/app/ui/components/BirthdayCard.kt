package com.remindly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.remindly.app.core.domain.model.Person
import com.remindly.app.ui.theme.AppRadius
import com.remindly.app.ui.theme.AppTypeScale
import com.remindly.app.ui.theme.AccentGreen
import com.remindly.app.ui.theme.PurplePrimary
import com.remindly.app.ui.theme.PurpleSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Birthday countdown card — PRD §27 (FAMILY · 37 DAYS · 30 October
 * · "Reminding daily at 10 PM") and birthday-day transformation §28.
 */
@Composable
fun BirthdayCard(
    person: Person,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    onClick: (() -> Unit)? = null,
) {
    val daysLeft = person.daysUntilBirthday(today)
    val isToday = daysLeft == 0
    val dateFmt = DateTimeFormatter.ofPattern("d MMMM")

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        radius = AppRadius.xl,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            if (isToday) {
                // PRD §28 birthday-day UI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppRadius.lg))
                        .background(Brush.horizontalGradient(listOf(PurplePrimary, PurpleSecondary)))
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "TODAY!",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = androidx.compose.ui.unit.sp(2f),
                        )
                        Text(
                            "${person.name}'s Birthday",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        person.birthDate?.let {
                            Text(
                                it.format(dateFmt),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            } else {
                Text(
                    (person.relationship.ifBlank { "Family" }).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = androidx.compose.ui.unit.sp(1.4f),
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PurpleSecondary, PurplePrimary)))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (person.photoUri == null) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (person.relationship.isNotBlank()) "${person.name}'s Birthday" else person.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        person.birthDate?.let {
                            Text(
                                it.format(dateFmt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${daysLeft ?: 0}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary,
                        )
                        Text(
                            if (daysLeft == 1) "DAY" else "DAYS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Text(
                        "Reminding daily at ${formatMinutes(person.reminderTime.hour * 60 + person.reminderTime.minute)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Compact person row for People list. */
@Composable
fun PersonCard(
    person: Person,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val daysLeft = person.daysUntilBirthday()
    SolidCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PurpleSecondary, PurplePrimary))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    person.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(person.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    person.relationship.ifBlank { person.birthDate?.format(DateTimeFormatter.ofPattern("d MMM")) ?: "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (daysLeft != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$daysLeft",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (daysLeft <= 7) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("d", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatMinutes(total: Int): String {
    val h = total / 60
    val m = total % 60
    val suffix = if (h >= 12) "PM" else "AM"
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "%d:%02d %s".format(h12, m, suffix)
}
