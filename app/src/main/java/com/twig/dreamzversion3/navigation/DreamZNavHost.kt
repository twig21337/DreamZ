package com.twig.dreamzversion3.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.twig.dreamzversion3.account.AccountRoute
import com.twig.dreamzversion3.dreams.DreamsRoute
import com.twig.dreamzversion3.dreamsigns.DreamSignsRoute
import com.twig.dreamzversion3.settings.SettingsRoute

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
        composable(DreamZDestination.Dreams.route) {
            DreamsRoute()
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
