package com.remindly.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.remindly.app.ui.theme.AppRadius
import com.remindly.app.ui.theme.PurplePrimary

/**
 * Pill UI (PRD §8) — heavily used for filters, chips, segmented controls,
 * date/time selectors. Not every button is a pill (avoid repetition).
 */

@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    gradient: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppRadius.pill)
    val interaction = remember { MutableInteractionSource() }
    val bg = when {
        gradient -> Brush.horizontalGradient(listOf(PurplePrimary, PurplePrimary.copy(alpha = 0.82f)))
        selected -> Brush.horizontalGradient(
            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
        )
        else -> Brush.horizontalGradient(
            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
        )
    }
    val fg = if (selected || gradient) Color.White else MaterialTheme.colorScheme.onSurface
    val border = if (selected || gradient) Color.Transparent
    else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))

    Row(
        modifier = modifier
            .shadow(if (selected || gradient) 6.dp else 0.dp, shape, ambientColor = PurplePrimary, spotColor = PurplePrimary)
            .clip(shape)
            .background(bg)
            .let { if (border != null) it.border(border, shape) else it }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick() }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) fg else fg.copy(alpha = 0.4f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/** Filter / category chip (PRD §8 examples: Today, Birthday, High Priority…). */
@Composable
fun PillChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    PillButton(
        text = text,
        modifier = modifier,
        selected = selected,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
        onClick = { onClick?.invoke() },
    )
}

/** Segmented pill control: [ Day ] [ Week ] [ Month ] (PRD §8). */
@Composable
fun SegmentedPills(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadius.pill))
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), RoundedCornerShape(AppRadius.pill))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            PillButton(
                text = label,
                selected = index == selectedIndex,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                onClick = { onSelect(index) },
            )
        }
    }
}

/** Date pill — raised when selected (PRD §7 calendar date skeuomorphism, §20). */
@Composable
fun DatePill(
    dayOfMonth: String,
    dayLabel: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppRadius.md)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .then(
                if (selected) Modifier.shadow(8.dp, shape, ambientColor = PurplePrimary, spotColor = PurplePrimary)
                else Modifier
            )
            .clip(shape)
            .background(
                if (selected) Brush.verticalGradient(listOf(PurplePrimary, PurplePrimary.copy(alpha = 0.85f)))
                else Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column {
            Text(
                dayOfMonth,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                dayLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Time pill (PRD §8 TimePill). */
@Composable
fun TimePill(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PillButton(text = "🕘  $text", modifier = modifier, onClick = onClick)
}
