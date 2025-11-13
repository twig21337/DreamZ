package com.twig.dreamzversion3.ui.onboarding

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.twig.dreamzversion3.R
import com.twig.dreamzversion3.auth.buildGoogleSignInClient
import com.twig.dreamzversion3.ui.theme.AuroraGradient
import com.twig.dreamzversion3.ui.theme.MidnightGradient
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val googleSignInClient = remember(context) { buildGoogleSignInClient(context) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.onboarding_drive_cancelled))
            }
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.onDriveConnected(account?.displayName, account?.email)
        } catch (apiException: ApiException) {
            val message = if (apiException.statusCode == CommonStatusCodes.CANCELED) {
                R.string.onboarding_drive_cancelled
            } else {
                R.string.onboarding_drive_failed
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(message))
            }
        }
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onFinished()
        }
    }

    val gradient = when (uiState.currentStep) {
        OnboardingStep.Welcome -> AuroraGradient
        OnboardingStep.ConnectDrive -> MidnightGradient
        OnboardingStep.Ready -> AuroraGradient
    }

    OnboardingScreen(
        uiState = uiState,
        gradient = gradient,
        snackbarHostState = snackbarHostState,
        onPrimaryAction = {
            when (uiState.currentStep) {
                OnboardingStep.Welcome -> viewModel.onStartPressed()
                OnboardingStep.ConnectDrive -> signInLauncher.launch(googleSignInClient.signInIntent)
                OnboardingStep.Ready -> viewModel.onFinish(onFinished)
            }
        },
        onSecondaryAction = {
            when (uiState.currentStep) {
                OnboardingStep.ConnectDrive -> viewModel.onDriveConnectionSkipped()
                else -> Unit
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun OnboardingScreen(
    uiState: OnboardingUiState,
    gradient: Brush,
    snackbarHostState: SnackbarHostState,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StepIndicators(current = uiState.currentStep)
                    AnimatedContent(
                        targetState = uiState.currentStep,
                        transitionSpec = {
                            (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) with
                                fadeOut())
                        },
                        label = "onboarding_step"
                    ) { step ->
                        StepContent(
                            step = step,
                            uiState = uiState,
                            onSecondaryAction = onSecondaryAction
                        )
                    }
                }
                PrimaryActionArea(
                    currentStep = uiState.currentStep,
                    onPrimaryAction = onPrimaryAction
                )
            }
        }
    }
}

@Composable
private fun StepIndicators(current: OnboardingStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OnboardingStep.values().forEach { step ->
            val isActive = step.ordinal <= current.ordinal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun StepContent(
    step: OnboardingStep,
    uiState: OnboardingUiState,
    onSecondaryAction: () -> Unit
) {
    when (step) {
        OnboardingStep.Welcome -> WelcomeStep()
        OnboardingStep.ConnectDrive -> ConnectDriveStep(uiState.driveConnected, onSecondaryAction)
        OnboardingStep.Ready -> ReadyStep(uiState.connectedAccountLabel)
    }
}

@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(id = R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(id = R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun ConnectDriveStep(
    driveConnected: Boolean,
    onSkip: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(id = R.string.onboarding_drive_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(id = R.string.onboarding_drive_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!driveConnected) {
            TextButton(onClick = onSkip) {
                Text(text = stringResource(id = R.string.onboarding_drive_skip))
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null)
                Text(
                    text = stringResource(id = R.string.onboarding_drive_connected),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ReadyStep(connectedAccount: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(id = R.string.onboarding_ready_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(id = R.string.onboarding_ready_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (connectedAccount.isNotBlank()) {
            Text(
                text = stringResource(id = R.string.onboarding_ready_connected_account, connectedAccount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun PrimaryActionArea(
    currentStep: OnboardingStep,
    onPrimaryAction: () -> Unit
) {
    val label = when (currentStep) {
        OnboardingStep.Welcome -> R.string.onboarding_start_button
        OnboardingStep.ConnectDrive -> R.string.onboarding_drive_connect_button
        OnboardingStep.Ready -> R.string.onboarding_finish_button
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onPrimaryAction,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = label))
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
