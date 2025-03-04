package com.example.ecommerceapp.presentation.profile.UserAddress.AddAddress

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
import java.util.UUID

class AddAddressViewModel : ViewModel() {

    // UI State for Add Address screen
    data class AddAddressUiState(
        val fullName: String = "",
        val phoneNumber: String = "",
        val address: String = "",
        val city: String = "",
        val state: String = "",
        val zipCode: String = "",
        val isDefault: Boolean = false,
        val isLoading: Boolean = false,
        val isSuccess: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(AddAddressUiState())
    val uiState: StateFlow<AddAddressUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()

    // Get current user ID or generate anonymous ID if not signed in
    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"

    // Update input fields
    fun updateFullName(name: String) {
        _uiState.update { it.copy(fullName = name) }
    }

    fun updatePhoneNumber(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone) }
    }

    fun updateAddress(address: String) {
        _uiState.update { it.copy(address = address) }
    }

    fun updateCity(city: String) {
        _uiState.update { it.copy(city = city) }
    }

    fun updateState(state: String) {
        _uiState.update { it.copy(state = state) }
    }

    fun updateZipCode(zipCode: String) {
        _uiState.update { it.copy(zipCode = zipCode) }
    }

    fun updateIsDefault(isDefault: Boolean) {
        _uiState.update { it.copy(isDefault = isDefault) }
    }

    // Save address to Firebase
    fun saveAddress() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val address = mapOf(
                    "fullName" to uiState.value.fullName,
                    "phoneNumber" to uiState.value.phoneNumber,
                    "address" to uiState.value.address,
                    "city" to uiState.value.city,
                    "state" to uiState.value.state,
                    "zipCode" to uiState.value.zipCode,
                    "isDefault" to uiState.value.isDefault,
                    "id" to UUID.randomUUID().toString()
                )

                // Reference to user's addresses
                val addressesRef = database.getReference("users").child(userId).child("addresses")

                // If this is set as default address, update all other addresses to non-default
                if (uiState.value.isDefault) {
                    // Get all current addresses
                    val snapshot = addressesRef.get().await()
                    snapshot.children.forEach { childSnapshot ->
                        addressesRef.child(childSnapshot.key!!).child("isDefault").setValue(false)
                    }
                }

                // Add new address
                val newAddressKey = addressesRef.push().key ?: UUID.randomUUID().toString()
                addressesRef.child(newAddressKey).setValue(address)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = "Failed to save address: ${e.message}"
                    )
                }
            }
        }
    }

    // Reset form fields after successful submission
    fun resetForm() {
        _uiState.update {
            AddAddressUiState(isSuccess = false)
        }
    }

    // Validate form before submission
    fun validateForm(): Boolean {
        val currentState = uiState.value
        return currentState.fullName.isNotBlank() &&
                currentState.phoneNumber.isNotBlank() &&
                currentState.address.isNotBlank() &&
                currentState.city.isNotBlank() &&
                currentState.state.isNotBlank() &&
                currentState.zipCode.isNotBlank()
    }
}