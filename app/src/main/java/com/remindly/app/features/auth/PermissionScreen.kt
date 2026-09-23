package com.remindly.app.features.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.remindly.app.R
import com.remindly.app.ui.components.GlassCard
import com.remindly.app.ui.components.PillButton
import com.remindly.app.ui.theme.AccentGreen
import com.remindly.app.ui.theme.PurplePrimary
import com.remindly.app.ui.theme.PurpleSecondary

/**
 * Permission onboarding — PRD §17 + §58:
 * explain WHY each permission is needed; never "allow everything".
 */
@Composable
fun PermissionScreen(
    onContinue: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    val context = LocalContext.current
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 33) {
                context.getSystemService(android.app.NotificationManager::class.java)
                    ?.areNotificationsEnabled() ?: false
            } else {
                androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }
    var exactGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(android.app.AlarmManager::class.java))?.canScheduleExactAlarms() ?: true
            } else true
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, Color(0xFFEDE6FF), MaterialTheme.colorScheme.background)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        Text("🔔", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.permissions_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(24.dp))

        PermissionRow(
            icon = Icons.Filled.NotificationsActive,
            tint = PurplePrimary,
            title = stringResource(R.string.permissions_notif_title),
            body = stringResource(R.string.permissions_notif_body),
            granted = notifGranted,
            cta = stringResource(R.string.enable_notifications),
            onCta = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Open app notification settings on older versions
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            )
                        }
                }
            },
        )
        Spacer(Modifier.height(14.dp))
        PermissionRow(
            icon = Icons.Filled.Timer,
            tint = PurpleSecondary,
            title = stringResource(R.string.permissions_alarm_title),
            body = stringResource(R.string.permissions_alarm_body),
            granted = exactGranted,
            cta = stringResource(R.string.permissions_alarm_title),
            onCta = {
                onOpenExactAlarmSettings()
            },
        )
        Spacer(Modifier.height(14.dp))
        PermissionRow(
            icon = Icons.Filled.BatteryFull,
            tint = AccentGreen,
            title = stringResource(R.string.permissions_battery_title),
            body = stringResource(R.string.permissions_battery_body),
            granted = false,
            cta = "Battery settings",
            onCta = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }.onFailure {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            },
        )

        Spacer(Modifier.height(32.dp))
        PillButton(
            text = stringResource(R.string.continue_label),
            modifier = Modifier.fillMaxWidth(),
            gradient = true,
            onClick = onContinue,
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    granted: Boolean,
    cta: String,
    onCta: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.7f)))),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    if (granted) Text("✓", color = AccentGreen, style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(8.dp))
            PillButton(
                text = if (granted) "Enabled" else cta,
                selected = granted,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                onClick = { if (!granted) onCta() },
            )
        }
    }
}

/** First-login profile setup — PRD §16 (minimal fields only). */
@Composable
fun ProfileSetupScreen(
    defaultName: String,
    onContinue: (name: String, defaultReminderMinutes: Int) -> Unit,
) {
    var name by remember { mutableStateOf(defaultName.ifBlank { "Akash" }) }
    var minutes by remember { mutableStateOf(21 * 60) }
    val timeLabel = "%d:%02d %s".format(
        ((minutes / 60) % 12).let { if (it == 0) 12 else it },
        minutes % 60,
        if (minutes >= 12 * 60) "PM" else "AM",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, Color(0xFFEDE6FF), MaterialTheme.colorScheme.background)
                )
            )
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.profile_welcome, name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.profile_personalize),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), radius = 28.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(stringResource(R.string.profile_name), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        focusedLabelColor = PurplePrimary,
                    ),
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.profile_default_time), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(9 * 60 to "9:00 AM", 21 * 60 to "9:00 PM", 22 * 60 to "10:00 PM").forEach { (m, label) ->
                        com.remindly.app.ui.components.PillChip(
                            text = label,
                            selected = minutes == m,
                            onClick = { minutes = m },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.profile_timezone), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    java.util.TimeZone.getDefault().id,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        PillButton(
            text = stringResource(R.string.continue_label),
            modifier = Modifier.fillMaxWidth(),
            gradient = true,
            onClick = { onContinue(name.ifBlank { "there" }, minutes) },
        )
    }
}
