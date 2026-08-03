package com.workout.tracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.workout.tracker.ui.daydetail.DayDetailScreen
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
    BottomNavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

private val tabTitles = listOf("Overview", "Workout Tracker", "Settings")

object Routes {
    const val MAIN = "main"
    const val DAY_DETAIL = "day/{dayId}"
    const val LOG_EXERCISE = "log/{exerciseId}"
    const val PROGRESS = "progress/{exerciseId}"
    const val WALKING = "walking"
    const val WALKING_PROGRESS = "walking_progress"

    fun dayDetail(dayId: Long) = "day/$dayId"
    fun logExercise(exerciseId: Long) = "log/$exerciseId"
    fun progress(exerciseId: Long) = "progress/$exerciseId"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController, Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                onDayClick = { navController.navigate(Routes.dayDetail(it)) },
                onExerciseClick = { navController.navigate(Routes.progress(it)) },
                onWalkingClick = { navController.navigate(Routes.WALKING) },
                onWalkingProgressClick = { navController.navigate(Routes.WALKING_PROGRESS) },
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    onDayClick: (Long) -> Unit,
    onExerciseClick: (Long) -> Unit,
    onWalkingClick: () -> Unit,
    onWalkingProgressClick: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var overviewRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(tabTitles[selectedTab]) },
                scrollBehavior = scrollBehavior,
            )
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
            2 -> SettingsScreen(
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
