package com.remindly.app.features.more

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remindly.app.R
import com.remindly.app.core.data.repo.SettingsRepository
import com.remindly.app.core.di.AppContainer
import com.remindly.app.ui.components.GlassCard
import com.remindly.app.ui.components.PillChip
import com.remindly.app.ui.components.SectionHeader
import com.remindly.app.ui.components.SolidCard
import com.remindly.app.ui.theme.AppRadius
import com.remindly.app.ui.theme.PurplePrimary
import com.remindly.app.ui.theme.PurpleSecondary
import kotlinx.coroutines.launch

/**
 * More tab — PRD §33:
 * profile card · Templates/Statistics/History · Appearance · Notifications ·
 * Sound · Widgets · Categories · Backup · Privacy · About.
 */
@Composable
fun MoreScreen(
    container: AppContainer,
    settings: SettingsRepository.Settings,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var themeMode by remember { mutableStateOf(settings.themeMode) }
    var followTz by remember { mutableStateOf(settings.followDeviceTz) }
    var notifOn by remember {
        mutableStateOf(
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        )
    }
    var exactOn by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
            } else true
        )
    }
    var reminderCount by remember { mutableIntStateOf(0) }
    var peopleCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        reminderCount = container.reminderRepository.getAll().size
        peopleCount = container.peopleRepository.getAll().size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp, end = 20.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
    ) {
        Text("More", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))

        // Profile card (PRD §33)
        GlassCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.xl) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PurpleSecondary, PurplePrimary))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        settings.userName.take(1).uppercase().ifBlank { "A" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        settings.userName.ifBlank { "Guest" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        when (settings.authProvider) {
                            "google" -> "Google Account"
                            "email" -> "Email Account"
                            else -> stringResource(R.string.guest_account)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(14.dp))
        // Quick stats
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Reminders", "$reminderCount", Modifier.weight(1f))
            StatCard("People", "$peopleCount", Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader("PREFERENCES")
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // Appearance (PRD §33 + §2 dark palette)
                Text(
                    stringResource(R.string.appearance),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                        PillChip(label, selected = themeMode == mode, onClick = {
                            themeMode = mode
                            scope.launch { container.settingsRepository.setThemeMode(mode) }
                        })
                    }
                }
                Spacer(Modifier.height(14.dp))

                SettingToggle(
                    stringResource(R.string.notifications_setting),
                    "Reminders & birthdays",
                    notifOn,
                ) {
                    if (!notifOn) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        runCatching { context.startActivity(intent) }
                            .onFailure {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                    } else {
                        notifOn = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                    }
                }

                SettingToggle(
                    "Exact alarms",
                    "Precise reminder timing",
                    exactOn,
                ) {
                    if (!exactOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    } else {
                        exactOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
                        } else true
                    }
                }

                SettingToggle(
                    "Follow device timezone",
                    "Reminders adapt when you travel (PRD §59)",
                    followTz,
                ) { scope.launch { container.settingsRepository.setFollowDeviceTz(!followTz); followTz = !followTz } }
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader("APP")
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                NavRow(stringResource(R.string.templates)) {}
                NavRow(stringResource(R.string.statistics)) {}
                NavRow(stringResource(R.string.notification_history)) {}
                NavRow(stringResource(R.string.widgets)) {}
                NavRow(stringResource(R.string.categories)) {}
                NavRow(stringResource(R.string.backup_sync)) {}
                NavRow(stringResource(R.string.privacy)) {}
                NavRow(stringResource(R.string.about)) {}
            }
        }

        Spacer(Modifier.height(14.dp))
        // Account actions (PRD §37 logout / account deletion)
        SolidCard(modifier = Modifier.fillMaxWidth(), radius = AppRadius.lg) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    stringResource(R.string.logout),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            scope.launch {
                                container.settingsRepository.logout()
                                // Restart to gates
                                val act = context as? android.app.Activity
                                act?.recreate()
                            }
                        }
                        .padding(vertical = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Remindly v1.0.0 · Offline-first",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = PurplePrimary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PurplePrimary,
            ),
        )
    }
}

@Composable
private fun NavRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
