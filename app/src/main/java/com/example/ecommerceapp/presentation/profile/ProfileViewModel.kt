package com.example.ecommerceapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileViewModel : ViewModel() {

    // UI State for Profile screen
    data class ProfileUiState(
        val name: String = "",
        val email: String = "",
        val profileImageUrl: String = "",
        val orderCount: Int = 0,
        val addressCount: Int = 0,
        val lastFourDigits: String = "",
        val reviewsCount: Int = 0,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()

    // Get current user ID or generate anonymous ID if not signed in
    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // If user is signed in, get data from Firebase
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Update basic information from Firebase Auth (but NOT the profile image)
            _uiState.update {
                it.copy(
//                    name = currentUser.displayName ?: "User 101",
                    email = currentUser.email ?: ""

                )
            }

            // Load the rest of user data including profile image from Realtime Database
            loadUserDataFromDatabase()
        } else {
            // Handle anonymous user or not signed in
            _uiState.update {
                it.copy(
                    name = "Guest User",
                    email = "Sign in to see your info",
                    isLoading = false
                )
            }
        }
    }

    private fun loadUserDataFromDatabase() {
        val userRef = database.getReference("users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {

                    val currentUser = auth.currentUser ?: return

                    // Get user name from database first, fallback to Auth display name
                    val userName = snapshot.child("name").getValue(String::class.java)
                        ?: currentUser.displayName
                        ?: "User 101"

                    // Get profile image URL from database
                    val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                    // Orders count
                    val ordersRef = database.getReference("orders").child(userId)
                    ordersRef.get().addOnSuccessListener { ordersSnapshot ->
                        val ordersCount = ordersSnapshot.childrenCount.toInt()
                        _uiState.update {
                            it.copy(
                                orderCount = ordersCount
                            )
                        }
                    }

                    // Addresses count
                    val addressesCount = snapshot.child("addresses").childrenCount.toInt()

                    // Payment method - get the last 4 digits if available
                    val paymentMethod = snapshot.child("paymentMethods").children.firstOrNull()
                    val lastFourDigits = paymentMethod?.child("lastFourDigits")?.getValue(String::class.java) ?: "34"

                    // Initialize reviews count
                    var reviewsCount = 0

                    // Count user reviews from comments node
                    val commentsRef = database.getReference("comments")
                    commentsRef.get().addOnSuccessListener { commentsSnapshot ->
                        var reviewCount = 0

                        // Loop through all product comments
                        for (productSnapshot in commentsSnapshot.children) {
                            // Check if user has a comment for this product
                            if (productSnapshot.hasChild(userId)) {
                                reviewCount++
                            }
                        }

                        // Update the reviewsCount with the calculated value
                        reviewsCount = reviewCount

                        // Update UI state with the review count
                        _uiState.update {
                            it.copy(
                                reviewsCount = reviewCount
                            )
                        }
                    }

                    _uiState.update {
                        it.copy(
                            name = userName,
                            profileImageUrl = profileImageUrl,
                            addressCount = addressesCount,
                            lastFourDigits = lastFourDigits,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load profile data: ${e.message}"
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            }
        })
    }
    // Function to update profile picture
    fun updateProfilePicture(base64Image: String) {
        viewModelScope.launch {
            try {
                // Update in Firebase Realtime Database if user is logged in
                auth.currentUser?.let { user ->
                    database.getReference("users").child(userId).child("profileImageUrl").setValue(base64Image)

                    // Update UI state
                    _uiState.update { it.copy(profileImageUrl = base64Image) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update profile picture: ${e.message}") }
            }
        }
    }
    //update username
    fun updateUsername(newName: String) {
        viewModelScope.launch {
            try {
                // Update in Firebase Realtime Database if user is logged in
                auth.currentUser?.let { user ->
                    database.getReference("users").child(userId).child("name").setValue(newName)

                    // Update UI state
                    _uiState.update { it.copy(name = newName) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update username: ${e.message}") }
            }
        }
    }

    // Function to sign out
    fun signOut() {
        auth.signOut()
        _uiState.update {
            ProfileUiState(
                name = "Guest User",
                email = "Sign in to see your info",
                isLoading = false
            )
        }
    }

    // Add placeholder data (for demo or testing)
    fun loadPlaceholderData() {
        _uiState.update {
            it.copy(
                name = "Matilda Brown",
                email = "matildabrown@mail.com",
                orderCount = 12,
                addressCount = 3,
                lastFourDigits = "34",
                reviewsCount = 4,
                isLoading = false
            )
        }
    }



}