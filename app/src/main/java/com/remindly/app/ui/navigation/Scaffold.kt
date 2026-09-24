package com.remindly.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.remindly.app.ui.theme.PurplePrimary

/** Destinations (PRD section 9 navigation, section 75 screen inventory). */
object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val PROFILE_SETUP = "profile_setup"
    const val PERMISSIONS = "permissions"
    const val HOME = "home"
    const val TASKS = "tasks"
    const val CALENDAR = "calendar"
    const val PEOPLE = "people"
    const val MORE = "more"
    const val QUICK_ADD = "quick_add"
    const val CREATE_REMINDER = "reminder/create?date={date}"
    const val EDIT_REMINDER = "reminder/edit/{id}"
    const val REMINDER_DETAIL = "reminder/detail/{id}"
    const val SEARCH = "search"
    const val ADD_PERSON = "person/add"
    const val PERSON_DETAIL = "person/{id}"

    fun editReminder(id: Long) = "reminder/edit/$id"
    fun reminderDetail(id: Long) = "reminder/detail/$id"
    fun personDetail(id: Long) = "person/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home),
    Tab(Routes.TASKS, "Tasks", Icons.Outlined.Inventory2),
    Tab(Routes.CALENDAR, "Calendar", Icons.Outlined.CalendarMonth),
    Tab(Routes.PEOPLE, "People", Icons.Outlined.People),
    Tab(Routes.MORE, "More", Icons.Outlined.Menu),
)

private val tabRoutes = tabs.map { it.route }.toSet()

/**
 * Bottom navigation + central elevated FAB (PRD section 9):
 *
 *   Home   Tasks   +   Calendar   People
 *
 * Uses plain Row/Box (no NavigationBar APIs) for maximum compatibility.
 */
@Composable
fun RemindlyScaffold(
    nav: NavHostController,
    onAddClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in tabRoutes

    val bottomPadding = if (showBar) 76.dp else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        content(
            PaddingValues(top = 0.dp, bottom = bottomPadding)
        )

        if (showBar) {
            // Bar background
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    NavItem(tabs[0], currentRoute, nav)
                    NavItem(tabs[1], currentRoute, nav)

                    // Center spacer for the floating +
                    Box(modifier = Modifier.size(56.dp))

                    NavItem(tabs[2], currentRoute, nav)
                    NavItem(tabs[3], currentRoute, nav)
                    NavItem(tabs[4], currentRoute, nav)
                }
            }

            // Elevated central FAB (PRD section 9)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
                    .size(58.dp)
                    .shadow(14.dp, CircleShape, ambientColor = PurplePrimary, spotColor = PurplePrimary)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PurplePrimary, Color(0xFF8B7CFF))))
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add reminder",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

@Composable
private fun NavItem(tab: Tab, currentRoute: String?, nav: NavHostController) {
    val selected = currentRoute == tab.route
    val tint = if (selected) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (currentRoute != tab.route) {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Shared screen scaffold with ambient gradient background. */
@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background,
                    )
                )
            ),
        verticalArrangement = Arrangement.Top,
        content = content,
    )
}