package com.remindly.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.remindly.app.core.data.repo.SettingsRepository
import com.remindly.app.core.di.AppContainer
import com.remindly.app.features.auth.LoginScreen
import com.remindly.app.features.auth.PermissionScreen
import com.remindly.app.features.auth.ProfileSetupScreen
import com.remindly.app.features.calendar.CalendarScreen
import com.remindly.app.features.home.HomeScreen
import com.remindly.app.features.more.MoreScreen
import com.remindly.app.features.onboarding.OnboardingScreen
import com.remindly.app.features.people.AddPersonScreen
import com.remindly.app.features.people.PeopleScreen
import com.remindly.app.features.people.PersonDetailScreen
import com.remindly.app.features.reminders.CreateEditReminderScreen
import com.remindly.app.features.reminders.QuickAddSheet
import com.remindly.app.features.reminders.ReminderDetailScreen
import com.remindly.app.features.reminders.SearchScreen
import com.remindly.app.features.tasks.TasksScreen

/**
 * Root navigation graph (PRD §9, §75).
 * Gates: Onboarding → Login → Profile → Permissions → Main tabs.
 */
@Composable
fun RemindlyNavHost(
    container: AppContainer,
    settings: SettingsRepository.Settings,
    deeplinkReminderId: Long = -1L,
) {
    val nav = rememberNavController()
    var showQuickAdd by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Repair scheduler state whenever we land on Home (PRD §57)
    LaunchedEffect(Unit) {
        container.scheduler.rebuildSchedules()
    }

    val startDestination = when {
        !settings.onboardingDone -> Routes.ONBOARDING
        !settings.signedIn && !settings.guestMode -> Routes.LOGIN
        !settings.permissionsDone -> Routes.PERMISSIONS
        else -> Routes.HOME
    }

    // Notification deep link → reminder detail (PRD §36)
    LaunchedEffect(deeplinkReminderId) {
        if (deeplinkReminderId > 0) {
            nav.navigate(Routes.reminderDetail(deeplinkReminderId))
        }
    }

    RemindlyScaffold(
        nav = nav,
        onAddClick = { showQuickAdd = true },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { slideInHorizontally(tween(250)) { it / 8 } + fadeIn(tween(250)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { slideOutHorizontally(tween(250)) { it / 8 } + fadeOut(tween(200)) },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        kotlinx.coroutines.runBlocking { container.settingsRepository.markOnboardingDone() }
                        nav.navigate(Routes.LOGIN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onGuest = { name ->
                        kotlinx.coroutines.runBlocking { container.settingsRepository.continueAsGuest(name) }
                        nav.navigate(Routes.PROFILE_SETUP) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onEmail = { name, email ->
                        kotlinx.coroutines.runBlocking {
                            container.settingsRepository.signIn("email", name, email)
                        }
                        nav.navigate(Routes.PROFILE_SETUP) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onGoogle = { name, email ->
                        // PRD §13 — platform Credential Manager flow would plug in here.
                        kotlinx.coroutines.runBlocking {
                            container.settingsRepository.signIn("google", name, email)
                        }
                        nav.navigate(Routes.PROFILE_SETUP) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.PROFILE_SETUP) {
                ProfileSetupScreen(
                    defaultName = settings.userName,
                    onContinue = { name, minutes ->
                        kotlinx.coroutines.runBlocking {
                            container.settingsRepository.completeProfile(name, minutes)
                        }
                        nav.navigate(Routes.PERMISSIONS) {
                            popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.PERMISSIONS) {
                PermissionScreen(
                    onContinue = {
                        kotlinx.coroutines.runBlocking { container.settingsRepository.markPermissionsDone() }
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.PERMISSIONS) { inclusive = true }
                        }
                    },
                    onOpenExactAlarmSettings = {
                        // PRD §17 exact alarms
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                            ).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    },
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    userName = settings.userName,
                    padding = padding,
                    onReminderClick = { id -> nav.navigate(Routes.reminderDetail(id)) },
                    onAddClick = { showQuickAdd = true },
                    onSearchClick = { nav.navigate(Routes.SEARCH) },
                )
            }
            composable(Routes.TASKS) {
                TasksScreen(
                    container = container,
                    padding = padding,
                    onReminderClick = { id -> nav.navigate(Routes.reminderDetail(id)) },
                    onAddClick = { showQuickAdd = true },
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    container = container,
                    padding = padding,
                    onReminderClick = { id -> nav.navigate(Routes.reminderDetail(id)) },
                )
            }
            composable(Routes.PEOPLE) {
                PeopleScreen(
                    container = container,
                    padding = padding,
                    onAddPerson = { nav.navigate(Routes.ADD_PERSON) },
                    onPersonClick = { id -> nav.navigate(Routes.personDetail(id)) },
                )
            }
            composable(Routes.MORE) {
                MoreScreen(
                    container = container,
                    settings = settings,
                    padding = padding,
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    container = container,
                    onBack = { nav.popBackStack() },
                    onReminderClick = { id -> nav.navigate(Routes.reminderDetail(id)) },
                )
            }
            composable(
                Routes.CREATE_REMINDER,
                arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true }),
            ) { entry ->
                CreateEditReminderScreen(
                    container = container,
                    reminderId = null,
                    presetDate = entry.arguments?.getString("date"),
                    onDone = { nav.popBackStack() },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                Routes.EDIT_REMINDER,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                CreateEditReminderScreen(
                    container = container,
                    reminderId = entry.arguments?.getLong("id"),
                    presetDate = null,
                    onDone = { nav.popBackStack() },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                Routes.REMINDER_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                ReminderDetailScreen(
                    container = container,
                    reminderId = entry.arguments?.getLong("id") ?: -1L,
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> nav.navigate(Routes.editReminder(id)) },
                    onDeleted = { nav.popBackStack() },
                )
            }
            composable(Routes.ADD_PERSON) {
                AddPersonScreen(
                    container = container,
                    personId = null,
                    onDone = { nav.popBackStack() },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                Routes.PERSON_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                PersonDetailScreen(
                    container = container,
                    personId = entry.arguments?.getLong("id") ?: -1L,
                    onBack = { nav.popBackStack() },
                    onEdit = { /* inline edit in v2 */ },
                )
            }
        }

        // Quick add bottom sheet (PRD §22) overlays whatever tab is open
        if (showQuickAdd) {
            QuickAddSheet(
                container = container,
                onDismiss = { showQuickAdd = false },
                onMoreOptions = {
                    showQuickAdd = false
                    nav.navigate("reminder/create?date=")
                },
                onCreated = { showQuickAdd = false },
            )
        }
    }
}
