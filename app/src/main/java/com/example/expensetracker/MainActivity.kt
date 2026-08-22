package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.example.expensetracker.ui.HomeScreen
import com.example.expensetracker.ui.ProfileScreen
import com.example.expensetracker.ui.StatsScreen
import com.example.expensetracker.ui.TransactionProfileScreen
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Stats : Screen("stats", "Stats", Icons.Default.BarChart)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)

    // Detail screen — not a tab, bottom bar hidden
    object TransactionProfile : Screen("transaction_profile", "")
}

class MainActivity : ComponentActivity() {
    private val viewModel by lazy { AppViewModel(application as ExpenseApp) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) { AppRoot(viewModel) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMonthlySummary()
    }
}

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Stats, Screen.Profile)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // Show bottom bar only on the main tabs,
    // not on detail screens like the transaction profile
    val showBottomBar = items.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { screen ->

                        NavigationBarItem(
                            icon = { screen.icon?.let { Icon(it, contentDescription = screen.label) } },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Home.route, modifier = Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    // Tap on a transaction row: set the selected
                    // counterparty on the ViewModel, then navigate
                    // to the detail screen.
                    onTransactionClick = { counterparty ->
                        viewModel.openTransactionProfile(counterparty)
                        navController.navigate(Screen.TransactionProfile.route)
                    }
                )
            }
            composable(Screen.Stats.route) { StatsScreen(viewModel) }
            composable(Screen.Profile.route) { ProfileScreen(viewModel) }
            composable(Screen.TransactionProfile.route) {
                TransactionProfileScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}