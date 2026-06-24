package com.scilog.app.presentation.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState == AuthState.SUCCESS) onAuthenticated()
    }

    fun launchBiometric() {
        val activity = context as? FragmentActivity ?: return
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            viewModel.onBiometricUnavailable()
            return
        }

        viewModel.onAuthenticating()
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                viewModel.onBiometricSuccess()
            override fun onAuthenticationFailed() = viewModel.onBiometricFailed()
            override fun onAuthenticationError(code: Int, msg: CharSequence) =
                viewModel.onBiometricFailed()
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock SciLog")
            .setSubtitle("Confirm your identity to continue")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    LaunchedEffect(Unit) { launchBiometric() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = "Biometric",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(52.dp)
                )
            }

            Text(
                text = "SciLog",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = when (authState) {
                    AuthState.AUTHENTICATING -> "Authenticating…"
                    AuthState.FAILED -> "Authentication failed. Try again."
                    AuthState.BIOMETRIC_UNAVAILABLE -> "Biometrics unavailable — tap to bypass"
                    else -> "Touch the sensor to unlock"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            when (authState) {
                AuthState.FAILED -> Button(onClick = { launchBiometric() }) {
                    Text("Retry")
                }
                AuthState.BIOMETRIC_UNAVAILABLE -> TextButton(onClick = onAuthenticated) {
                    Text("Continue without biometrics")
                }
                AuthState.AUTHENTICATING -> CircularProgressIndicator(modifier = Modifier.size(32.dp))
                else -> {}
            }
        }
    }
}
