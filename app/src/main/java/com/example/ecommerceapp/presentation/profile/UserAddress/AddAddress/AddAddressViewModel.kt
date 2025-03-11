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
    //validate full name
    private fun validateFullName(): Boolean {
        val fullName = uiState.value.fullName.trim()

        if (fullName.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Full Name must not be empty"
                )
            }
            return false
        }

        if (fullName.any { it.isDigit() }) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Full Name should not contain numbers"
                )
            }
            return false
        }

        return true
    }


    fun updatePhoneNumber(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone) }
    }

    //validate phone number
    fun validatePhoneNumber(): Boolean {
        if (uiState.value.phoneNumber.isEmpty() || uiState.value.phoneNumber.length < 10) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Phone number must not be empty" +
                            " and must be 10 digits"
                )
            }
            return false
        }
        return true
    }

    fun updateAddress(address: String) {
        _uiState.update { it.copy(address = address) }
    }

    //validate address
    private fun validateAddress(): Boolean {
        if (uiState.value.address.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Address must not be empty"
                )
            }
            return false
        }
        return true
    }

    fun updateCity(city: String) {
        _uiState.update { it.copy(city = city) }
    }

    //validate city
    private fun validateCity(): Boolean {
        if (uiState.value.city.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "City must not be empty"
                )
            }
            return false
        }
        return true
    }

    fun updateState(state: String) {
        _uiState.update { it.copy(state = state) }
    }

    //validate state
    private fun validateState(): Boolean {
        if (uiState.value.state.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "State must not be empty"
                )
            }
            return false
        }
        return true
    }

    fun updateZipCode(zipCode: String) {
        _uiState.update { it.copy(zipCode = zipCode) }
    }

    //validate zip code
    fun validateZipCode(): Boolean {
        if (uiState.value.zipCode.isEmpty() || uiState.value.zipCode.length < 6) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Zip code must not be empty" +
                            " and must be 6 digits"
                )
            }
            return false
        }
        return true
    }

    fun updateIsDefault(isDefault: Boolean) {
        _uiState.update { it.copy(isDefault = isDefault) }
    }

    // Save address to Firebase
    fun saveAddress() {
        if (!validateFullName()){
            return
        }
        if (!validatePhoneNumber()){
            return
        }
        if (!validateAddress()){
            return
        }
        if (!validateCity()){
            return
        }
        if (!validateState()){
            return
        }
        if (!validateZipCode()){
            return
        }
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

}