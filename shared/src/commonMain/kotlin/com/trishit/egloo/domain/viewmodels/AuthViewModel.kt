package com.trishit.egloo.domain.viewmodels

import com.trishit.egloo.data.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val error: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(private val authRepo: AuthRepository) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) = _uiState.update { it.copy(email = email, error = null) }
    fun onPasswordChanged(password: String) = _uiState.update { it.copy(password = password, error = null) }
    fun onFullNameChanged(name: String) = _uiState.update { it.copy(fullName = name, error = null) }

    fun login(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        scope.launch {
            val result = authRepo.login(_uiState.value.email, _uiState.value.password)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                onSuccess()
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Login failed") }
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        scope.launch {
            val result = authRepo.register(
                _uiState.value.email,
                _uiState.value.password,
                _uiState.value.fullName
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Registration failed") }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
