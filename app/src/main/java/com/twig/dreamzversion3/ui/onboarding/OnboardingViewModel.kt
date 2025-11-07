package com.twig.dreamzversion3.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.twig.dreamzversion3.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onStartPressed() {
        advanceStep()
    }

    fun onDriveConnected(accountName: String?, accountEmail: String?) {
        _uiState.update {
            it.copy(
                driveConnected = true,
                connectedAccountLabel = listOfNotNull(accountName, accountEmail)
                    .joinToString(" • ")
                    .ifBlank { accountEmail ?: accountName ?: "" }
            )
        }
        advanceStep()
    }

    fun onDriveConnectionSkipped() {
        advanceStep()
    }

    fun onFinish(onFinished: () -> Unit) {
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
            _uiState.update { it.copy(isCompleted = true) }
            onFinished()
        }
    }

    fun reset() {
        _uiState.value = OnboardingUiState()
    }

    private fun advanceStep() {
        _uiState.update { state ->
            val nextStep = when (state.currentStep) {
                OnboardingStep.Welcome -> OnboardingStep.ConnectDrive
                OnboardingStep.ConnectDrive -> OnboardingStep.Ready
                OnboardingStep.Ready -> OnboardingStep.Ready
            }
            state.copy(currentStep = nextStep)
        }
    }

    companion object {
        fun factory(preferences: UserPreferencesRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { OnboardingViewModel(preferences) }
        }
    }
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val driveConnected: Boolean = false,
    val connectedAccountLabel: String = "",
    val isCompleted: Boolean = false
)

enum class OnboardingStep {
    Welcome,
    ConnectDrive,
    Ready
}
