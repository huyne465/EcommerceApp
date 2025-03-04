package com.example.ecommerceapp.presentation.sign_up

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecommerceapp.model.UserModel
import com.example.ecommerceapp.model.getCurrentFormattedDate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SignUpUiState(
    val email: String = "",
    val name: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val errorMessage: String? = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

class SignUpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    fun updateEmail(email: String) {
        _uiState.update { currentState ->
            currentState.copy(
                email = email
            )
        }
    }

    fun updateName(name: String) {
        _uiState.update { currentState ->
            currentState.copy(
                name = name
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

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { currentState ->
            currentState.copy(
                confirmPassword = confirmPassword
            )
        }
    }

    private fun validateEmail():Boolean {
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

    private fun validateName():Boolean {
        if (uiState.value.name.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Name must not be empty"
                )
            }
            return false
        }
        return true
    }

    private fun validatePassword():Boolean {
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

    private fun validateConfirmPassword():Boolean {
        if (uiState.value.password != uiState.value.confirmPassword) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Password does not match"
                )
            }
            return false
        }
        return true
    }

    // Trong SignUpViewModel.kt
    fun signUp(email: String, name:String, password: String, confirmPassword: String){
        if(!validateEmail()){
            return
        }

        if(!validateName()){
            return
        }

        if(!validatePassword()){
            return
        }

        if(!validateConfirmPassword()){
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

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid

                if (userId != null) {
                    // Tạo mô hình người dùng để lưu vào Realtime Database
                    val user = UserModel(
                        email = email,
                        name = name,
                        password = password,
                        confirmPassword = confirmPassword,
                        createdAt = getCurrentFormattedDate()
                    )
                    // Lưu dữ liệu người dùng vào Realtime Database
                    saveUserToDatabase(userId, user)
                }

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
    fun resetSignUpState() {
        _uiState.update { currentState ->
            currentState.copy(
                isSuccess = false
            )
        }
    }

    private suspend fun saveUserToDatabase(userId: String, user: UserModel): Boolean {
        return try {

            // Lấy tham chiếu đến nút "users" trong cơ sở dữ liệu
            val database =
                FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
            val usersRef = database.getReference("users")

            // Lưu dữ liệu người dùng vào nút con với khóa là userId
            usersRef.child(userId).setValue(user).await()

            true
        } catch (e: Exception) {
            false
        }
    }


}