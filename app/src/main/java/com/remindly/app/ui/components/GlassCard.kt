package com.remindly.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.remindly.app.ui.theme.AppRadius
import com.remindly.app.ui.theme.AppShadows
import com.remindly.app.ui.theme.GlassBorderLight
import com.remindly.app.ui.theme.GlassFillDark
import com.remindly.app.ui.theme.GlassFillLight
import com.remindly.app.ui.theme.PurplePrimary
import com.remindly.app.ui.theme.PurpleSecondary

/**
 * Glass card (PRD §6 Glassmorphism Rules):
 * translucent fill · 1px light border · soft shadow · large radius.
 * Use for floating dashboard cards, filters, quick actions — NOT forms.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = AppRadius.xl,
    dark: Boolean = MaterialTheme.colorScheme.background.luminanceCompat() < 0.5f,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val fill = if (dark) GlassFillDark else GlassFillLight
    val border = borderColor ?: if (dark) Color.White.copy(alpha = 0.12f) else GlassBorderLight
    val shape = RoundedCornerShape(radius)
    var base = modifier
        .shadow(
            elevation = AppShadows.cardElevation,
            shape = shape,
            ambientColor = PurplePrimary.copy(alpha = 0.25f),
            spotColor = PurplePrimary.copy(alpha = 0.25f),
        )
        .clip(shape)
        .background(fill)
        .border(BorderStroke(1.dp, border), shape)
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        base = base.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    }
    Box(modifier = base, content = content)
}

/** Solid elevated surface — forms, dialogs, accessibility-critical UI (§6). */
@Composable
fun SolidCard(
    modifier: Modifier = Modifier,
    radius: Dp = AppRadius.card,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    var base = modifier
        .shadow(elevation = 4.dp, shape = shape)
        .clip(shape)
        .background(color)
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        base = base.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    }
    Box(modifier = base, content = content)
}

/** Micro-skeuomorphic raised surface (PRD §7) — selected calendar dates etc. */
@Composable
fun RaisedSurface(
    modifier: Modifier = Modifier,
    radius: Dp = AppRadius.md,
    color: Color = PurplePrimary,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = shape, ambientColor = color, spotColor = color)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.88f))))
            .padding(2.dp),
        content = content,
    )
}

/** Background gradient layer used behind every screen (PRD §1). */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    MaterialTheme.colorScheme.background,
                )
            )
        )
    )
}

private fun Color.luminanceCompat(): Float =
    (0.299f * red + 0.587f * green + 0.114f * blue)
