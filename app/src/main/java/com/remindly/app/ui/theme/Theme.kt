package com.remindly.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// PRD §2 palettes
private val LightColors = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PurpleSecondary,
    onPrimaryContainer = Color.White,
    secondary = PurpleSecondary,
    onSecondary = Color.White,
    tertiary = AccentGreen,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextDark,
    surface = SurfaceLight,
    onSurface = TextDark,
    surfaceVariant = CardWhite,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = CardWhite,
    surfaceContainerHigh = CardWhite,
    outline = Color(0xFFE4DFF7),
    error = Danger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBright,
    onPrimary = Color.White,
    primaryContainer = PurplePrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = PurpleSecondary,
    onSecondary = Color.White,
    tertiary = AccentGreen,
    onTertiary = Color.Black,
    background = BackgroundDark,
    onBackground = TextOnDark,
    surface = SurfaceDark,
    onSurface = TextOnDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = SecondaryOnDark,
    surfaceContainer = CardDark,
    surfaceContainerHigh = CardDark,
    outline = Color(0xFF3A3752),
    error = Danger,
    onError = Color.White,
)

/** Ambient gradient blobs behind content (PRD §1 Soft Glass layering). */
@Composable
fun ambientBackground(dark: Boolean = isSystemInDarkTheme()): List<Color> =
    if (dark) listOf(BackgroundDark, Color(0xFF1A1631), Color(0xFF141228))
    else listOf(BackgroundLight, Color(0xFFEDE6FF), Color(0xFFF7F1FF))

@Composable
fun ambientBrush(): Brush = Brush.verticalGradient(colors = ambientBackground())

@Composable
fun RemindlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
