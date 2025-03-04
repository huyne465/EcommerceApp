package com.example.ecommerceapp.presentation.sign_in

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val errorMessage: String? = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val hasUser: Boolean = false,
)

class SignInViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()


    init {
        _uiState.update { currentState ->
            currentState.copy(
                hasUser = (auth.currentUser != null)
            )
        }
    }

    fun updateEmail(email: String) {
        _uiState.update { currentState ->
            currentState.copy(
                email = email
            )
        }
    }

    fun updatePassword(password: String) {
        _uiState.update { currentState ->
            currentState.copy(
                password = password
            )
        }
    }

    private fun validateEmail(): Boolean {
        if (uiState.value.email.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Email must not be empty"
                )
            }
            return false
        }
        return true
    }

    private fun validatePassword(): Boolean {
        if (uiState.value.password.length < 6) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Password must be at least 6 characters"
                )
            }
            return false
        }
        return true
    }

    fun signIn(email: String, password: String) {
        if (!validateEmail()) {
            return
        }

        if (!validatePassword()) {
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = true,
                        errorMessage = "" // Xóa thông báo lỗi cũ nếu có
                    )
                }
                auth.signInWithEmailAndPassword(email, password).await()

                // Đặt isSuccess = true và không đặt lại thành false ngay sau đó
                _uiState.update { currentState ->
                    currentState.copy(
                        isSuccess = true,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        errorMessage = e.localizedMessage,
                        isLoading = false
                    )
                }
            }
        }
    }

    // Thêm hàm mới để reset trạng thái sau khi đã điều hướng
    fun resetSignInState() {
        _uiState.update { currentState ->
            currentState.copy(
                isSuccess = false,
                hasUser = false
            )
        }
    }

}