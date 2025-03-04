package com.example.ecommerceapp.presentation.profile.UserAddress.EditAddress


import android.util.Log
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

class EditAddressViewModel : ViewModel() {

    data class EditAddressUiState(
        val addressId: String = "",
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

    private val _uiState = MutableStateFlow(EditAddressUiState())
    val uiState: StateFlow<EditAddressUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()

    // Get current user ID
    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous_${java.util.UUID.randomUUID()}"

    // Load address data for editing
    fun loadAddress(addressId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, addressId = addressId) }
                Log.d("EditAddressViewModel", "Loading address: $addressId")

                val addressRef = database.getReference("users").child(userId).child("addresses").child(addressId)
                val snapshot = addressRef.get().await()

                if (snapshot.exists()) {
                    val fullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
                    val phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                    val address = snapshot.child("address").getValue(String::class.java) ?: ""
                    val city = snapshot.child("city").getValue(String::class.java) ?: ""
                    val state = snapshot.child("state").getValue(String::class.java) ?: ""
                    val zipCode = snapshot.child("zipCode").getValue(String::class.java) ?: ""
                    val isDefault = snapshot.child("isDefault").getValue(Boolean::class.java) ?: false

                    _uiState.update {
                        it.copy(
                            fullName = fullName,
                            phoneNumber = phoneNumber,
                            address = address,
                            city = city,
                            state = state,
                            zipCode = zipCode,
                            isDefault = isDefault,
                            isLoading = false
                        )
                    }
                    Log.d("EditAddressViewModel", "Address loaded successfully")
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Address not found") }
                    Log.e("EditAddressViewModel", "Address not found: $addressId")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load address: ${e.message}") }
                Log.e("EditAddressViewModel", "Error loading address", e)
            }
        }
    }

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

    // Save updated address to Firebase
    fun saveAddress() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val addressData = mapOf(
                    "fullName" to uiState.value.fullName,
                    "phoneNumber" to uiState.value.phoneNumber,
                    "address" to uiState.value.address,
                    "city" to uiState.value.city,
                    "state" to uiState.value.state,
                    "zipCode" to uiState.value.zipCode,
                    "isDefault" to uiState.value.isDefault
                )

                // Reference to the specific address
                val addressRef = database.getReference("users").child(userId)
                    .child("addresses").child(uiState.value.addressId)

                // If this is set as default address, update all other addresses to non-default
                if (uiState.value.isDefault) {
                    val addressesRef = database.getReference("users").child(userId).child("addresses")
                    val snapshot = addressesRef.get().await()

                    // Set all addresses to non-default
                    snapshot.children.forEach { child ->
                        if (child.key != uiState.value.addressId) {
                            addressesRef.child(child.key!!).child("isDefault").setValue(false)
                        }
                    }
                }

                // Update the address
                addressRef.updateChildren(addressData).await()

                Log.d("EditAddressViewModel", "Address updated successfully")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("EditAddressViewModel", "Error updating address", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = "Failed to update address: ${e.message}"
                    )
                }
            }
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