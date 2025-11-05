package com.twig.dreamzversion3.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.twig.dreamzversion3.account.AccountRoute
import com.twig.dreamzversion3.dreamsigns.DreamSignsRoute
import com.twig.dreamzversion3.settings.SettingsRoute
import com.twig.dreamzversion3.ui.dreams.DreamEntryRoute
import com.twig.dreamzversion3.ui.dreams.DreamsDestinations
import com.twig.dreamzversion3.ui.dreams.DreamsListRoute
import com.twig.dreamzversion3.ui.dreams.DreamsViewModel

@Composable
fun DreamZNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = DreamZDestination.Dreams.route,
        modifier = modifier
    ) {
        navigation(
            route = DreamZDestination.Dreams.route,
            startDestination = DreamsDestinations.LIST_ROUTE
        ) {
            composable(DreamsDestinations.LIST_ROUTE) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(DreamZDestination.Dreams.route)
                }
                val dreamsViewModel: DreamsViewModel = viewModel(parentEntry)
                DreamsListRoute(
                    onAddDream = { navController.navigate(DreamsDestinations.ADD_ROUTE) },
                    viewModel = dreamsViewModel
                )
            }
            composable(DreamsDestinations.ADD_ROUTE) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(DreamZDestination.Dreams.route)
                }
                val dreamsViewModel: DreamsViewModel = viewModel(parentEntry)
                DreamEntryRoute(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = dreamsViewModel
                )
            }
        }
        composable(DreamZDestination.DreamSigns.route) {
            DreamSignsRoute()
        }
        composable(DreamZDestination.Settings.route) {
            SettingsRoute()
        }
        composable(DreamZDestination.Account.route) {
            AccountRoute()
        }
    }
}
