package com.twig.dreamzversion3.data

import androidx.compose.runtime.staticCompositionLocalOf

val LocalUserPreferencesRepository = staticCompositionLocalOf<UserPreferencesRepository> {
    error("UserPreferencesRepository not provided")
}
