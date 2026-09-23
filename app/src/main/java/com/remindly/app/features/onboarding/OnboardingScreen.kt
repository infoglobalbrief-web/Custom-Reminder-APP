package com.remindly.app.features.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.remindly.app.R
import com.remindly.app.ui.components.GlassCard
import com.remindly.app.ui.components.PillButton
import com.remindly.app.ui.theme.PurplePrimary
import com.remindly.app.ui.theme.PurpleSecondary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val titleRes: Int,
    val bodyRes: Int,
    val icons: List<ImageVector>,
)

private val pages = listOf(
    OnboardingPage(R.string.onboarding_1_title, R.string.onboarding_1_body, listOf(Icons.Filled.Event, Icons.Filled.Notifications, Icons.Filled.CheckCircle)),
    OnboardingPage(R.string.onboarding_2_title, R.string.onboarding_2_body, listOf(Icons.Filled.Notifications, Icons.Filled.Cake, Icons.Filled.Event)),
    OnboardingPage(R.string.onboarding_3_title, R.string.onboarding_3_body, listOf(Icons.Filled.CheckCircle, Icons.Filled.Cake, Icons.Filled.Event)),
)

/**
 * Onboarding — PRD §11: exactly 3 screens, Get Started CTA.
 * Illustration hero area ≈ 25–35% of screen (PRD §19).
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFFEDE6FF),
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
            .padding(horizontal = 24.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinished) {
                Text(stringResource(R.string.skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val data = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Illustration hero (PRD §19 ~30% of hero area)
                GlassCard(modifier = Modifier.size(220.dp), radius = 28.dp) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            data.icons.forEachIndexed { i, icon ->
                                Box(
                                    modifier = Modifier
                                        .size(if (i == 1) 72.dp else 56.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            if (i == 1) Brush.linearGradient(listOf(PurplePrimary, PurpleSecondary))
                                            else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.7f)))
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (i == 1) Color.White else PurplePrimary,
                                        modifier = Modifier.size(if (i == 1) 36.dp else 28.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
                Text(
                    stringResource(data.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(data.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }

        // Page indicator
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            pages.indices.forEach { i ->
                val active = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (active) 22.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (active) PurplePrimary else PurplePrimary.copy(alpha = 0.25f)),
                )
            }
        }

        PillButton(
            text = stringResource(if (lastPage) R.string.get_started else R.string.next),
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
            gradient = true,
            onClick = {
                if (lastPage) onFinished()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
        )
    }
}
