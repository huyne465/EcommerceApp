package com.example.ecommerceapp.presentation.profile.Setting.changePassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException


data class ChangePasswordUiState(
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
)

class ChangePasswordViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState

    fun updateOldPassword(oldPassword: String) {
        _uiState.update { it.copy(oldPassword = oldPassword) }
    }

    fun updateNewPassword(newPassword: String) {
        _uiState.update { it.copy(newPassword = newPassword) }
    }

    fun updateConfirmNewPassword(confirmNewPassword: String) {
        _uiState.update { it.copy(confirmNewPassword = confirmNewPassword) }
    }

    private fun validateOldPassword(): Boolean {
        if (uiState.value.oldPassword.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Current password must not be empty") }
            return false
        }
        return true
    }

    private fun validateNewPassword(): Boolean {
        val password = uiState.value.newPassword

        if (password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "New password must not be empty") }
            return false
        }

        if (password.length < 8) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 8 characters long") }
            return false
        }

        // Check for a mix of uppercase, lowercase, numbers, and special characters
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        if (!(hasUppercase && hasLowercase && hasDigit && hasSpecialChar)) {
            _uiState.update { it.copy(errorMessage = "Password must include uppercase, lowercase, number, and special character") }
            return false
        }

        return true
    }

    private fun validateConfirmNewPassword(): Boolean {
        if (uiState.value.confirmNewPassword.isEmpty() || uiState.value.confirmNewPassword != uiState.value.newPassword) {
            _uiState.update { it.copy(errorMessage = "Confirm new password must not be empty and must match the new password") }
            return false
        }
        return true
    }

    fun changePassword() {
        val currentPassword = uiState.value.oldPassword
        val newPassword = uiState.value.newPassword

        if (!validateOldPassword()) {
            return
        }
        if (!validateNewPassword()) {
            return
        }
        if (!validateConfirmNewPassword()) {
            return
        }
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user != null && user.email != null) {
                    val email = user.email!!
                    val credential = EmailAuthProvider.getCredential(email, currentPassword)

                    try {
                        // Re-authenticate
                        suspendCancellableCoroutine<Unit> { continuation ->
                            user.reauthenticate(credential).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    continuation.resume(Unit) {}
                                } else {
                                    val exception = task.exception
                                    when {
                                        exception?.message?.contains("invalid") == true -> {
                                            _uiState.update { it.copy(errorMessage = "Incorrect current password. Please try again.") }
                                        }

                                        exception?.message?.contains("malformed") == true ||
                                                exception?.message?.contains("expired") == true -> {
                                            _uiState.update { it.copy(errorMessage = "Authentication session expired. Please sign in again.") }
                                        }

                                        else -> {
                                            continuation.resumeWithException(
                                                exception ?: Exception(
                                                    "Authentication failed"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Update password
                        suspendCancellableCoroutine<Unit> { continuation ->
                            user.updatePassword(newPassword).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Update the password in the database for this child user
                                    val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
                                    val userId = user.uid
                                    database.getReference("users").child(userId).child("password").setValue(newPassword)
                                        .addOnSuccessListener {
                                            continuation.resume(Unit) {}
                                            _uiState.update {
                                                it.copy(
                                                    successMessage = "Password changed successfully",
                                                    oldPassword = "",
                                                    newPassword = "",
                                                    confirmNewPassword = ""
                                                )
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            continuation.resumeWithException(exception)
                                        }
                                } else {
                                    continuation.resumeWithException(
                                        task.exception ?: Exception("Failed to change password")
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "Error: ${e.message}") }
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "User not logged in") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
