package com.twig.dreamzversion3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twig.dreamzversion3.data.LocalUserPreferencesRepository
import com.twig.dreamzversion3.data.ThemeMode
import com.twig.dreamzversion3.data.UserPreferencesRepository
import com.twig.dreamzversion3.data.userPreferencesDataStore
import com.twig.dreamzversion3.navigation.DreamZDestination
import com.twig.dreamzversion3.navigation.DreamZDestinations
import com.twig.dreamzversion3.navigation.DreamZNavHost
import com.twig.dreamzversion3.ui.theme.ColorCombo
import com.twig.dreamzversion3.ui.theme.DreamZVersion3Theme
import com.twig.dreamzversion3.ui.theme.AuroraGradient
import com.twig.dreamzversion3.ui.theme.MidnightGradient
import com.twig.dreamzversion3.ui.onboarding.OnboardingRoute
import com.twig.dreamzversion3.ui.onboarding.OnboardingViewModel

@Composable
fun DreamZApp(appState: DreamZAppState = rememberDreamZAppState()) {
    val context = LocalContext.current
    val preferences: UserPreferencesRepository = remember(context) {
        UserPreferencesRepository(context.userPreferencesDataStore)
    }
    val themeMode by preferences.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val colorCombo by preferences.colorComboFlow.collectAsState(initial = ColorCombo.AURORA)
    val onboardingCompleted by preferences.onboardingCompletedFlow.collectAsState(initial = false)
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.factory(preferences)
    )
    val backgroundBrush = when {
        colorCombo == ColorCombo.AURORA && useDarkTheme -> MidnightGradient
        colorCombo == ColorCombo.AURORA && !useDarkTheme -> AuroraGradient
        else -> colorCombo.backgroundBrush(useDarkTheme)
    }

    CompositionLocalProvider(LocalUserPreferencesRepository provides preferences) {
        DreamZVersion3Theme(darkTheme = useDarkTheme, colorCombo = colorCombo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
            ) {
                if (!onboardingCompleted) {
                    OnboardingRoute(
                        onFinished = {},
                        viewModel = onboardingViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Scaffold(
                        containerColor = Color.Transparent,
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
