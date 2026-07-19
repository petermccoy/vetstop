package com.vetstop.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vetstop.app.ui.screens.area.AreaMapScreen
import com.vetstop.app.ui.screens.locations.LocationDetailScreen
import com.vetstop.app.ui.screens.locations.LocationsScreen
import com.vetstop.app.ui.screens.trip.TripPlannerScreen
import com.vetstop.app.ui.screens.settings.SettingsScreen
import com.vetstop.app.ui.screens.visit.LogVisitScreen
import com.vetstop.app.ui.screens.visits.RecentVisitsScreen

object Routes {
    const val AREAS = "areas"
    const val LOCATIONS = "locations"
    const val TRIP = "trip"
    const val VISITS = "visits"
    const val SETTINGS = "settings"
    const val LOCATION_DETAIL = "location/{placeId}"
    const val LOG_VISIT = "logVisit/{placeId}?visitId={visitId}"

    fun locationDetail(placeId: String) = "location/$placeId"

    /** Pass a [visitId] to edit an existing visit instead of logging a new one. */
    fun logVisit(placeId: String, visitId: Long = -1L) = "logVisit/$placeId?visitId=$visitId"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.AREAS, "Area", Icons.Filled.Map),
    TopLevelDestination(Routes.LOCATIONS, "Places", Icons.Filled.Place),
    TopLevelDestination(Routes.TRIP, "Trip", Icons.Filled.Route),
    TopLevelDestination(Routes.VISITS, "Visits", Icons.Filled.History),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun VetStopNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOCATIONS,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.AREAS) {
                AreaMapScreen()
            }
            composable(Routes.LOCATIONS) {
                LocationsScreen(
                    onLocationClick = { placeId ->
                        navController.navigate(Routes.locationDetail(placeId))
                    },
                )
            }
            composable(
                route = Routes.LOCATION_DETAIL,
                arguments = listOf(navArgument("placeId") { type = NavType.StringType }),
            ) {
                LocationDetailScreen(
                    onLogVisit = { placeId ->
                        navController.navigate(Routes.logVisit(placeId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.LOG_VISIT,
                arguments = listOf(
                    navArgument("placeId") { type = NavType.StringType },
                    navArgument("visitId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) {
                LogVisitScreen(
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Routes.TRIP) {
                TripPlannerScreen()
            }
            composable(Routes.VISITS) {
                RecentVisitsScreen(
                    onOpenVisitForm = { placeId, visitId ->
                        navController.navigate(Routes.logVisit(placeId, visitId))
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
