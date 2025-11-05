package com.twig.dreamzversion3

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Stable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.twig.dreamzversion3.navigation.DreamZDestination
import com.twig.dreamzversion3.navigation.DreamZDestinations
import com.twig.dreamzversion3.navigation.DreamZNavHost
import com.twig.dreamzversion3.ui.theme.DreamZVersion3Theme

@Composable
fun DreamZApp(appState: DreamZAppState = rememberDreamZAppState()) {
    DreamZVersion3Theme {
        Scaffold(
            bottomBar = { DreamZBottomBar(appState) }
        ) { innerPadding ->
            DreamZNavHost(
                navController = appState.navController,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Stable
class DreamZAppState(
    val navController: NavHostController
) {
    fun navigateTo(destination: DreamZDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
fun rememberDreamZAppState(
    navController: NavHostController = rememberNavController()
): DreamZAppState {
    return remember(navController) {
        DreamZAppState(navController)
    }
}

@Composable
private fun DreamZBottomBar(appState: DreamZAppState) {
    val navBackStackEntry by appState.navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        DreamZDestinations.forEach { destination ->
            val selected = currentDestination.isDestinationInHierarchy(destination.route)
            NavigationBarItem(
                selected = selected,
                onClick = { appState.navigateTo(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelResId)
                    )
                },
                label = { Text(text = stringResource(destination.labelResId)) },
                alwaysShowLabel = true
            )
        }
    }
}

private fun NavDestination?.isDestinationInHierarchy(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}
