package com.twig.dreamzversion3.account

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AccountViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState
}

data class AccountUiState(
    val title: String = "Account",
    val description: String = "Manage authentication, backups, and personal details."
)
