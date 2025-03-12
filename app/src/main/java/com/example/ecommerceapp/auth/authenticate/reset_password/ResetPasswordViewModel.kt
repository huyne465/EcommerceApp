package com.example.ecommerceapp.auth.authenticate.reset_password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ResetPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ResetPasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun resetPassword() {
        if (!validateEmail()) {
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                auth.sendPasswordResetEmail(_uiState.value.email).await()
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to send reset email"
                    )
                }
            }
        }
    }

    private fun validateEmail(): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

        return when {
            _uiState.value.email.isEmpty() -> {
                _uiState.update { it.copy(errorMessage = "Email cannot be empty") }
                false
            }
            !_uiState.value.email.matches(emailPattern.toRegex()) -> {
                _uiState.update { it.copy(errorMessage = "Please enter a valid email") }
                false
            }
            else -> true
        }
    }
}