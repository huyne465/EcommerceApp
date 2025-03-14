package com.example.ecommerceapp.presentation.manageUser
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecommerceapp.model.UserModel
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ManageUserViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ManageUserUiState())
    val uiState: StateFlow<ManageUserUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val usersRef = database.getReference("users")

    // Load all users from Firebase
    fun loadUsers() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val snapshot = usersRef.get().await()

                val users = mutableListOf<UserModel>()
                snapshot.children.forEach { userSnapshot ->
                    val user = userSnapshot.getValue(UserModel::class.java)
                    if (user != null) {
                        // Ensure the user ID is set
                        user.id = userSnapshot.key ?: ""

                        // Get profile image URL from child node if available
                        val profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)
                        if (!profileImageUrl.isNullOrEmpty()) {
                            user.photoUrl = profileImageUrl
                        }

                        users.add(user)
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        users = users
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load users: ${e.message}"
                    )
                }
            }
        }
    }

    // Toggle ban status for a user
    fun toggleUserBanStatus(userId: String, currentBanStatus: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true) }

                // Update the banned status in Firebase
                usersRef.child(userId).child("banned").setValue(!currentBanStatus).await()

                // Update the local list
                _uiState.update { state ->
                    val updatedUsers = state.users.map { user ->
                        if (user.id == userId) {
                            user.copy(banned = !currentBanStatus)
                        } else {
                            user
                        }
                    }

                    state.copy(
                        isProcessing = false,
                        users = updatedUsers,
                        actionMessage = if (!currentBanStatus)
                            "User has been banned"
                        else
                            "User has been unbanned"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Failed to update user: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class ManageUserUiState(
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val users: List<UserModel> = emptyList(),
    val errorMessage: String? = null,
    val actionMessage: String? = null
)