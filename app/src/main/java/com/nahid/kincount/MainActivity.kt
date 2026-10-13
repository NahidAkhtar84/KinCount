package com.nahid.kincount

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nahid.kincount.ui.add.AddEditScreen
import com.nahid.kincount.ui.daily.DailyScreen
import com.nahid.kincount.ui.dashboard.DashboardScreen
import com.nahid.kincount.ui.theme.KinCountTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KinCountTheme {
                KinCountRoot()
            }
        }
    }
}

/** Bottom-nav tabs. Add sits center; it becomes the mic FAB in M3 (PLAN.md §6.3). */
private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Dashboard", Icons.Filled.Home),
    Add("add", "Add", Icons.Filled.Add),
    Daily("daily", "Daily", Icons.Filled.DateRange),
}

private const val ROUTE_EDIT = "edit/{txId}"

@Composable
private fun KinCountRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { navController.navigateTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Tab.Dashboard.route) { DashboardScreen() }
            composable(Tab.Add.route) {
                AddEditScreen(
                    txId = null,
                    onDone = { navController.navigateTab(Tab.Daily) },
                )
            }
            composable(Tab.Daily.route) {
                DailyScreen(
                    onEdit = { txId -> navController.navigate("$ROUTE_EDIT_PREFIX$txId") },
                )
            }
            composable(ROUTE_EDIT) { backStackEntry ->
                AddEditScreen(
                    txId = backStackEntry.arguments?.getString("txId"),
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

private const val ROUTE_EDIT_PREFIX = "edit/"

private fun NavHostController.navigateTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
