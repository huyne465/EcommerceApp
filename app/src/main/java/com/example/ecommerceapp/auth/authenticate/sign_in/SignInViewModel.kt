package com.example.ecommerceapp.auth.authenticate.sign_in

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val usersRef = database.getReference("users")

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
                        errorMessage = ""
                    )
                }

                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid

                // Check if user is banned
                if (userId != null) {
                    val userBanSnapshot = usersRef.child(userId).child("banned").get().await()
                    val isBanned = userBanSnapshot.getValue(Boolean::class.java) ?: false

                    if (isBanned) {
                        // If banned, sign them out and show error
                        auth.signOut()
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                errorMessage = "Your account has been banned. Please contact support."
                            )
                        }
                        return@launch
                    }
                }

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


    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isEmpty()) {
            onError("Please enter your email address")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { currentState ->
                    currentState.copy(isLoading = true)
                }
                auth.sendPasswordResetEmail(email).await()
                _uiState.update { currentState ->
                    currentState.copy(isLoading = false)
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(isLoading = false)
                }
                onError(e.localizedMessage ?: "Failed to send reset email")
            }
        }
    }


}