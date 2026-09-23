package com.remindly.app.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.remindly.app.R
import com.remindly.app.ui.components.GlassCard
import com.remindly.app.ui.components.PillButton
import com.remindly.app.ui.theme.PurplePrimary

/**
 * Login screen — PRD §12–15:
 * Guest · Google · Apple · Email. Recognizable platform branding;
 * native Credential Manager / Sign-in SDK plugs into AuthRepository.
 */
@Composable
fun LoginScreen(
    onGuest: (name: String) -> Unit,
    onEmail: (name: String, email: String) -> Unit,
    onGoogle: (name: String, email: String) -> Unit,
) {
    var showEmailDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFFE9E1FF),
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Wordmark (PRD §10 / §15)
        Text("✦", style = MaterialTheme.typography.displaySmall, color = PurplePrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(36.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), radius = 28.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Recognizable Google button (PRD §15 — platform branding retained)
                PlatformButton(
                    label = stringResource(R.string.continue_google),
                    background = Color.White,
                    contentColor = Color(0xFF1F1F1F),
                    leading = {
                        Text("G", fontWeight = FontWeight.Bold, color = Color(0xFF4285F4), style = MaterialTheme.typography.titleMedium)
                    },
                ) {
                    // PRD §13: native Google authentication UI (Credential Manager).
                    // Demo signs in locally; swap in the platform SDK for release.
                    onGoogle("Akash", "akash@gmail.com")
                }
                Spacer(Modifier.height(12.dp))
                PlatformButton(
                    label = stringResource(R.string.continue_apple),
                    background = Color.Black,
                    contentColor = Color.White,
                    leading = {
                        Text("", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    },
                ) {
                    // PRD §14 iOS parity — enabled in the iOS build target.
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
                    Text(
                        "  ${stringResource(R.string.or_divider)}  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Email, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    TextButton(onClick = { showEmailDialog = true }) {
                        Text(
                            stringResource(R.string.continue_email),
                            color = PurplePrimary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                TextButton(onClick = { onGuest("Akash") }) {
                    Text(
                        stringResource(R.string.continue_guest),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    if (showEmailDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text(stringResource(R.string.continue_email)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.profile_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            focusedLabelColor = PurplePrimary,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showEmailDialog = false
                    onEmail(name.ifBlank { "there" }, email)
                }) { Text(stringResource(R.string.continue_label), color = PurplePrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
            shape = RoundedCornerShape(24.dp),
        )
    }
}

@Composable
private fun PlatformButton(
    label: String,
    background: Color,
    contentColor: Color,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            leading?.invoke()
            if (leading != null) Spacer(Modifier.size(8.dp))
            Text(
                label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
