package com.remindly.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Design tokens — PRD §2 Primary visual palette
// ---------------------------------------------------------------------------

// Light
val PurplePrimary = Color(0xFF6658E8)
val PurplePrimaryDark = Color(0xFF5144C9)
val PurpleSecondary = Color(0xFFA77AF3)
val AccentGreen = Color(0xFF7BC99A)
val BackgroundLight = Color(0xFFF4F1FF)
val SurfaceLight = Color(0xB8FFFFFF)      // rgba(255,255,255,0.72)
val CardWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF17152B)
val TextSecondary = Color(0xFF77758A)

// Dark
val BackgroundDark = Color(0xFF11101B)
val SurfaceDark = Color(0xBF252336)       // rgba(37,35,54,0.75)
val CardDark = Color(0xFF211F30)
val PrimaryBright = Color(0xFF8B7CFF)
val TextOnDark = Color(0xFFF7F5FF)
val SecondaryOnDark = Color(0xFFAAA6BD)

// Glass card (PRD §6)
val GlassBorderLight = Color(0x8CFFFFFF)  // rgba(255,255,255,0.55)
val GlassFillLight = Color(0x99FFFFFF)    // rgba(255,255,255,0.60)
val GlassFillDark = Color(0x59211F30)

// Semantic helpers
val Danger = Color(0xFFE5636C)
val WarningAmber = Color(0xFFF3A55A)
val PriorityHigh = Color(0xFFE5636C)
val PriorityLow = Color(0xFF9B98AD)

// Gradient stops for ambient blobs (PRD §1 soft gradient backgrounds)
val GradientStart = Color(0xFF8B7CFF)
val GradientMid = Color(0xFFC3A6F7)
val GradientEnd = Color(0xFF9FD8C0)
