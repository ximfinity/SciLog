package com.scilog.app.presentation.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

enum class AuthState { IDLE, AUTHENTICATING, SUCCESS, FAILED, BIOMETRIC_UNAVAILABLE }

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.IDLE)
    val authState: StateFlow<AuthState> = _authState

    fun onBiometricSuccess() { _authState.value = AuthState.SUCCESS }
    fun onBiometricFailed() { _authState.value = AuthState.FAILED }
    fun onBiometricUnavailable() { _authState.value = AuthState.BIOMETRIC_UNAVAILABLE }
    fun onAuthenticating() { _authState.value = AuthState.AUTHENTICATING }
    fun reset() { _authState.value = AuthState.IDLE }
}
