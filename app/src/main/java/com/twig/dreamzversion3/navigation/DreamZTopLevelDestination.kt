package com.twig.dreamzversion3.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.twig.dreamzversion3.R

sealed class DreamZDestination(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int
) {
    data object Dreams : DreamZDestination(
        route = "dreams",
        icon = Icons.Outlined.AutoAwesome,
        labelResId = R.string.dreams_tab_label
    )

    data object DreamSigns : DreamZDestination(
        route = "dream_signs",
        icon = Icons.Outlined.Visibility,
        labelResId = R.string.dream_signs_tab_label
    )

    data object Settings : DreamZDestination(
        route = "settings",
        icon = Icons.Outlined.Settings,
        labelResId = R.string.settings_tab_label
    )

    data object Account : DreamZDestination(
        route = "account",
        icon = Icons.Outlined.AccountCircle,
        labelResId = R.string.account_tab_label
    )
}

val DreamZDestinations = listOf(
    DreamZDestination.Dreams,
    DreamZDestination.DreamSigns,
    DreamZDestination.Settings,
    DreamZDestination.Account
)
