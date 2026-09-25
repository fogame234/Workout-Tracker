package com.workout.tracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.workout.tracker.ui.daydetail.DayDetailScreen
import com.workout.tracker.ui.exercises.ExerciseInfoScreen
import com.workout.tracker.ui.exercises.ExerciseLibraryScreen
import com.workout.tracker.ui.home.HomeScreen
import com.workout.tracker.ui.logexercise.LogExerciseScreen
import com.workout.tracker.ui.overview.OverviewScreen
import com.workout.tracker.ui.progress.ProgressScreen
import com.workout.tracker.ui.settings.SettingsScreen
import com.workout.tracker.ui.walking.WalkingProgressScreen
import com.workout.tracker.ui.walking.WalkingScreen

private data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem("Overview", Icons.Filled.Insights, Icons.Outlined.Insights),
    BottomNavItem("Workouts", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    BottomNavItem("Exercises", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    BottomNavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

private val tabTitles = listOf("Overview", "Workout Tracker", "Exercises", "Settings")

object Routes {
    const val MAIN = "main"
    const val DAY_DETAIL = "day/{dayId}"
    const val LOG_EXERCISE = "log/{exerciseId}"
    const val PROGRESS = "progress/{exerciseId}"
    const val WALKING = "walking"
    const val WALKING_PROGRESS = "walking_progress"
    const val EXERCISE_INFO = "exercise_info/{exerciseId}"

    fun dayDetail(dayId: Long) = "day/$dayId"
    fun logExercise(exerciseId: Long) = "log/$exerciseId"
    fun progress(exerciseId: Long) = "progress/$exerciseId"
    fun exerciseInfo(exerciseId: Long) = "exercise_info/$exerciseId"
}

/** Duration of the push and pop animation between destinations. */
private const val NAV_TRANSITION_MS = 300

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        // Slide rather than the default crossfade, which let the background show through.
        enterTransition = { slideIntoContainer(SlideDirection.Left, tween(NAV_TRANSITION_MS)) },
        exitTransition = { slideOutOfContainer(SlideDirection.Left, tween(NAV_TRANSITION_MS)) },
        popEnterTransition = { slideIntoContainer(SlideDirection.Right, tween(NAV_TRANSITION_MS)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.Right, tween(NAV_TRANSITION_MS)) },
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                onDayClick = { navController.navigate(Routes.dayDetail(it)) },
                onExerciseClick = { navController.navigate(Routes.progress(it)) },
                onWalkingClick = { navController.navigate(Routes.WALKING) },
                onWalkingProgressClick = { navController.navigate(Routes.WALKING_PROGRESS) },
                onExerciseInfoClick = { navController.navigate(Routes.exerciseInfo(it)) },
            )
        }
        composable(
            Routes.DAY_DETAIL,
            arguments = listOf(navArgument("dayId") { type = NavType.LongType }),
        ) {
            DayDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogExercise = { navController.navigate(Routes.logExercise(it)) },
                onViewProgress = { navController.navigate(Routes.progress(it)) },
            )
        }
        composable(
            Routes.LOG_EXERCISE,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
        ) {
            LogExerciseScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            Routes.PROGRESS,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
        ) {
            ProgressScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.WALKING) {
            WalkingScreen(
                onNavigateBack = { navController.popBackStack() },
                onViewProgress = { navController.navigate(Routes.WALKING_PROGRESS) },
            )
        }
        composable(Routes.WALKING_PROGRESS) {
            WalkingProgressScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            Routes.EXERCISE_INFO,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
        ) {
            ExerciseInfoScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    onDayClick: (Long) -> Unit,
    onExerciseClick: (Long) -> Unit,
    onWalkingClick: () -> Unit,
    onWalkingProgressClick: () -> Unit,
    onExerciseInfoClick: (Long) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var overviewRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(tabTitles[selectedTab]) })
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            if (index == 0) overviewRefreshKey++
                            selectedTab = index
                        },
                        icon = {
                            Icon(
                                if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> OverviewScreen(
                onExerciseClick = onExerciseClick,
                onWalkingProgressClick = onWalkingProgressClick,
                refreshTrigger = overviewRefreshKey,
                modifier = Modifier.padding(innerPadding),
            )
            1 -> HomeScreen(
                onDayClick = onDayClick,
                onWalkingClick = onWalkingClick,
                modifier = Modifier.padding(innerPadding),
            )
            2 -> ExerciseLibraryScreen(
                onExerciseClick = onExerciseInfoClick,
                modifier = Modifier.padding(innerPadding),
            )
            3 -> SettingsScreen(
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
