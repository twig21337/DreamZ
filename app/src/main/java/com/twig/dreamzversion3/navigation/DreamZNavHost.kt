package com.twig.dreamzversion3.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twig.dreamzversion3.account.AccountRoute
import com.twig.dreamzversion3.data.LocalUserPreferencesRepository
import com.twig.dreamzversion3.data.dream.DreamRepositories
import com.twig.dreamzversion3.dreamsigns.DreamSignIgnoredWordsRoute
import com.twig.dreamzversion3.dreamsigns.DreamSignsDestinations
import com.twig.dreamzversion3.dreamsigns.DreamSignsRoute
import com.twig.dreamzversion3.dreamsigns.DreamSignsViewModel
import com.twig.dreamzversion3.drive.DriveSyncManager
import com.twig.dreamzversion3.drive.DriveSyncStateRepository
import com.twig.dreamzversion3.settings.SettingsScreen
import com.twig.dreamzversion3.settings.SettingsViewModel
import com.twig.dreamzversion3.ui.dreams.DreamEntryRoute
import com.twig.dreamzversion3.ui.dreams.DreamsDestinations
import com.twig.dreamzversion3.ui.dreams.DreamsListRoute
import com.twig.dreamzversion3.ui.dreams.DreamsViewModel
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import com.twig.dreamzversion3.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DreamZNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dreamRepository = remember(context) { DreamRepositories.persistent(context) }
    val preferences = LocalUserPreferencesRepository.current
    val driveSyncStateRepository = remember(context) { DriveSyncStateRepository(context) }
    val dreamsViewModelFactory = remember(dreamRepository, preferences) {
        DreamsViewModel.factory(dreamRepository, preferences)
    }

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
                val dreamsViewModel: DreamsViewModel = viewModel(
                    parentEntry,
                    factory = dreamsViewModelFactory
                )
                val snackbarHostState = remember { SnackbarHostState() }
                val snackbarMessages = backStackEntry.savedStateHandle.getStateFlow(
                    DreamsDestinations.SNACKBAR_RESULT_KEY,
                    null as String?
                )
                LaunchedEffect(snackbarMessages) {
                    snackbarMessages.collectLatest { message ->
                        if (!message.isNullOrBlank()) {
                            snackbarHostState.showSnackbar(message)
                            backStackEntry.savedStateHandle[DreamsDestinations.SNACKBAR_RESULT_KEY] = null
                        }
                    }
                }
                DreamsListRoute(
                    onAddDream = { navController.navigate(DreamsDestinations.ADD_ROUTE) },
                    onDreamSelected = { dreamId ->
                        navController.navigate(DreamsDestinations.editRoute(dreamId))
                    },
                    snackbarHostState = snackbarHostState,
                    viewModel = dreamsViewModel
                )
            }
            composable(DreamsDestinations.ADD_ROUTE) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(DreamZDestination.Dreams.route)
                }
                val dreamsViewModel: DreamsViewModel = viewModel(
                    parentEntry,
                    factory = dreamsViewModelFactory
                )
                DreamEntryRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onShowMessage = { message ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            DreamsDestinations.SNACKBAR_RESULT_KEY,
                            message
                        )
                    },
                    dreamId = null,
                    viewModel = dreamsViewModel
                )
            }
            composable(
                route = DreamsDestinations.EDIT_ROUTE,
                arguments = listOf(
                    navArgument(DreamsDestinations.DREAM_ID_ARG) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(DreamZDestination.Dreams.route)
                }
                val dreamsViewModel: DreamsViewModel = viewModel(
                    parentEntry,
                    factory = dreamsViewModelFactory
                )
                val dreamId =
                    backStackEntry.arguments?.getString(DreamsDestinations.DREAM_ID_ARG)
                DreamEntryRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onShowMessage = { message ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            DreamsDestinations.SNACKBAR_RESULT_KEY,
                            message
                        )
                    },
                    dreamId = dreamId,
                    viewModel = dreamsViewModel
                )
            }
        }
        navigation(
            route = DreamZDestination.DreamSigns.route,
            startDestination = DreamSignsDestinations.HOME_ROUTE
        ) {
            composable(DreamSignsDestinations.HOME_ROUTE) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(DreamZDestination.DreamSigns.route)
                }
                val preferences = LocalUserPreferencesRepository.current
                val viewModelFactory = remember(preferences, dreamRepository) {
                    DreamSignsViewModel.factory(
                        repository = dreamRepository,
                        preferences = preferences
                    )
                }
                val viewModel: DreamSignsViewModel = viewModel(
                    parentEntry,
                    factory = viewModelFactory
                )
                DreamSignsRoute(
                    onManageIgnoredWords = {
                        navController.navigate(DreamSignsDestinations.IGNORED_ROUTE)
                    },
                    viewModel = viewModel
                )
            }
            composable(DreamSignsDestinations.IGNORED_ROUTE) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(DreamZDestination.DreamSigns.route)
                }
                val preferences = LocalUserPreferencesRepository.current
                val viewModelFactory = remember(preferences, dreamRepository) {
                    DreamSignsViewModel.factory(
                        repository = dreamRepository,
                        preferences = preferences
                    )
                }
                val viewModel: DreamSignsViewModel = viewModel(
                    parentEntry,
                    factory = viewModelFactory
                )
                DreamSignIgnoredWordsRoute(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }
        }
        composable(DreamZDestination.Settings.route) {
            val preferences = LocalUserPreferencesRepository.current
            val driveSyncManager = remember(dreamRepository, driveSyncStateRepository) {
                DriveSyncManager(dreamRepository, driveSyncStateRepository)
            }
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(
                    preferences = preferences,
                    driveSyncManager = driveSyncManager
                )
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current
            val lastSyncedCount = remember { mutableIntStateOf(-1) }
            LaunchedEffect(uiState.lastSyncedCount) {
                val count = uiState.lastSyncedCount ?: return@LaunchedEffect
                if (count != lastSyncedCount.intValue) {
                    val message = if (count > 0) {
                        context.getString(R.string.account_drive_sync_success, count)
                    } else {
                        context.getString(R.string.account_drive_sync_empty)
                    }
                    snackbarHostState.showSnackbar(message)
                    lastSyncedCount.intValue = count
                }
            }
            SettingsScreen(
                uiState = uiState,
                onThemeSelected = viewModel::onThemeSelected,
                onColorComboSelected = viewModel::onColorComboSelected,
                onDriveTokenChanged = viewModel::onDriveTokenChange,
                onConnectDrive = viewModel::connectDrive,
                onDisconnectDrive = viewModel::disconnectDrive,
                onSyncNow = viewModel::syncNow,
                snackbarHostState = snackbarHostState
            )
        }
        composable(DreamZDestination.Account.route) {
            AccountRoute()
        }
    }
}
