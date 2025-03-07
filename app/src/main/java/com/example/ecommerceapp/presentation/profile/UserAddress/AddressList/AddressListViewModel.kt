package com.example.ecommerceapp.presentation.profile.UserAddress.AddressList


import android.util.Log
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

class AddressListViewModel : ViewModel() {

    data class Address(
        var id: String = "",
        val fullName: String = "",
        val phoneNumber: String = "",
        val address: String = "",
        val city: String = "",
        val state: String = "",
        val zipCode: String = "",
        val isDefault: Boolean = false
    )

    data class AddressListUiState(
        val addresses: List<Address> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(AddressListUiState())
    val uiState: StateFlow<AddressListUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()

    // Get current user ID or generate anonymous ID if not signed in
    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"

    init {
        loadAddresses()
    }

    fun loadAddresses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val addressesRef = database.getReference("users").child(userId).child("addresses")

            addressesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val addressList = mutableListOf<Address>()

                        for (addressSnapshot in snapshot.children) {
                            val id = addressSnapshot.key ?: ""
                            val fullName = addressSnapshot.child("fullName").getValue(String::class.java) ?: ""
                            val phoneNumber = addressSnapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                            val addressText = addressSnapshot.child("address").getValue(String::class.java) ?: ""
                            val city = addressSnapshot.child("city").getValue(String::class.java) ?: ""
                            val state = addressSnapshot.child("state").getValue(String::class.java) ?: ""
                            val zipCode = addressSnapshot.child("zipCode").getValue(String::class.java) ?: ""
                            val isDefault = addressSnapshot.child("isDefault").getValue(Boolean::class.java) ?: false

                            val address = Address(
                                id = id,
                                fullName = fullName,
                                phoneNumber = phoneNumber,
                                address = addressText,
                                city = city,
                                state = state,
                                zipCode = zipCode,
                                isDefault = isDefault
                            )

                            addressList.add(address)
                        }

                        // Sort addresses - default address first, then alphabetically by name
                        val sortedAddresses = addressList.sortedWith(
                            compareByDescending<Address> { it.isDefault }
                                .thenBy { it.fullName }
                        )

                        _uiState.update {
                            it.copy(
                                addresses = sortedAddresses,
                                isLoading = false
                            )
                        }

                        Log.d("AddressListViewModel", "Loaded ${sortedAddresses.size} addresses")
                    } catch (e: Exception) {
                        Log.e("AddressListViewModel", "Error loading addresses", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to load addresses: ${e.message}"
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
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            try {
                val addressRef = database.getReference("users").child(userId).child("addresses").child(addressId)
                addressRef.removeValue()

                // After deletion, the ValueEventListener will automatically update the list
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to delete address: ${e.message}")
                }
            }
        }
    }

    fun setDefaultAddress(addressId: String) {
        viewModelScope.launch {
            try {
                val addressesRef = database.getReference("users").child(userId).child("addresses")

                // First set all addresses to non-default
                for (address in uiState.value.addresses) {
                    addressesRef.child(address.id).child("isDefault").setValue(false)
                }

                // Then set the selected address as default
                addressesRef.child(addressId).child("isDefault").setValue(true)

                // The ValueEventListener will automatically update the list
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to update default address: ${e.message}")
                }
            }
        }
    }
}