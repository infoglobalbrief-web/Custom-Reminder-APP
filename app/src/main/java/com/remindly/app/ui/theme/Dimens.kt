package com.remindly.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Spacing tokens — PRD §61 Design Tokens (no hard-coded magic numbers)
// ---------------------------------------------------------------------------
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

// ---------------------------------------------------------------------------
// Corner radius tokens — PRD §5
// ---------------------------------------------------------------------------
object AppRadius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val card = 20.dp   // Main cards 20–28px
    val lg = 22.dp
    val xl = 28.dp
    val button = 16.dp // Buttons 14–18px
    val pill = 999.dp
}

// ---------------------------------------------------------------------------
// Typography scale — PRD §4 (compact, strong hierarchy)
// ---------------------------------------------------------------------------
object AppTypeScale {
    val heroTitle = 30.sp        // 26–32
    val screenTitle = 24.sp      // 22–26
    val cardTitle = 16.sp        // 15–17
    val body = 14.sp             // 13–15
    val metadata = 12.sp         // 11–12
    val caption = 11.sp          // 10–11
    val time = 18.sp             // 16–22
    val largeCountdown = 38.sp   // 32–42
}

// ---------------------------------------------------------------------------
// Motion — PRD §62 (subtle; productivity app)
// ---------------------------------------------------------------------------
object AppMotion {
    const val pageTransitionMs = 250
    const val bottomSheetMs = 300
}

// ---------------------------------------------------------------------------
// Shadows — PRD §6 soft shadows
// ---------------------------------------------------------------------------
object AppShadows {
    val cardElevation = 6.dp
    val floatingElevation = 12.dp
}
