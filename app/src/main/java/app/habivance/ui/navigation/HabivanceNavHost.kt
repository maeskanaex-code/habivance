package app.habivance.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.habivance.ui.detail.HabitDetailScreen
import app.habivance.ui.edit.HabitEditScreen
import app.habivance.ui.list.HabitListScreen
import app.habivance.ui.settings.SettingsScreen

object Routes {
    const val LIST = "list"
    const val EDIT = "edit"
    const val EDIT_WITH_ID = "edit/{habitId}"
    const val DETAIL = "detail/{habitId}"
    const val SETTINGS = "settings"
    fun editWithId(habitId: Long) = "edit/$habitId"
    fun detailWithId(habitId: Long) = "detail/$habitId"
}

@Composable
fun HabivanceNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIST,
        modifier = modifier
    ) {
        composable(Routes.LIST) {
            HabitListScreen(
                onAddHabit = { navController.navigate(Routes.EDIT) },
                onEditHabit = { id -> navController.navigate(Routes.detailWithId(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("habitId") { type = NavType.LongType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: 0L
            HabitDetailScreen(
                habitId = habitId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.editWithId(id)) },
                onDeleted = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.EDIT_WITH_ID,
            arguments = listOf(navArgument("habitId") { type = NavType.LongType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: 0L
            HabitEditScreen(
                habitId = habitId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.EDIT) {
            HabitEditScreen(
                habitId = 0L,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
