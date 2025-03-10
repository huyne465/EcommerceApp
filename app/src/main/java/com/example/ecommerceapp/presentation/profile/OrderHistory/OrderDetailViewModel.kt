package com.example.ecommerceapp.presentation.profile.OrderHistory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecommerceapp.model.Order
import com.example.ecommerceapp.model.OrderItem
import com.example.ecommerceapp.model.PaymentDetails
import com.example.ecommerceapp.model.ShippingAddress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OrderDetailViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState

    fun loadOrderDetails(orderId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = OrderDetailUiState(isLoading = true)
                val userId = auth.currentUser?.uid

                if (userId == null) {
                    _uiState.value = OrderDetailUiState(errorMessage = "User not logged in")
                    return@launch
                }

                // Path was incorrect - orders are stored directly under "orders/userId" not "users/userId/orders"
                val snapshot = database.reference
                    .child("orders")
                    .child(userId)
                    .child(orderId)
                    .get()
                    .await()

                if (!snapshot.exists()) {
                    Log.e("OrderDetailViewModel", "Order not found: $orderId")
                    _uiState.value = OrderDetailUiState(errorMessage = "Order not found")
                    return@launch
                }

                val order = parseOrder(snapshot)
                if (order != null) {
                    Log.d("OrderDetailViewModel", "Loaded order details for: $orderId")
                    _uiState.value = OrderDetailUiState(order = order)
                } else {
                    _uiState.value = OrderDetailUiState(errorMessage = "Failed to parse order data")
                }
            } catch (e: Exception) {
                Log.e("OrderDetailViewModel", "Error loading order details", e)
                _uiState.value = OrderDetailUiState(errorMessage = "Error: ${e.message}")
            }
        }
    }

    private fun parseOrder(snapshot: DataSnapshot): Order? {
        try {
            val orderId = snapshot.key ?: return null

            val items = mutableListOf<OrderItem>()
            val itemsSnapshot = snapshot.child("items")
            for (itemSnapshot in itemsSnapshot.children) {
                val brand = itemSnapshot.child("brand").getValue(String::class.java) ?: ""
                val imageUrl = itemSnapshot.child("imageUrl").getValue(String::class.java) ?: ""
                val name = itemSnapshot.child("name").getValue(String::class.java) ?: ""
                val price = itemSnapshot.child("price").getValue(Double::class.java) ?: 0.0
                val productId = itemSnapshot.child("productId").getValue(String::class.java) ?: ""
                val quantity = itemSnapshot.child("quantity").getValue(Int::class.java) ?: 0

                items.add(OrderItem(brand, imageUrl, name, price, productId, quantity))
            }

            // Get status
            val status = snapshot.child("status").getValue(String::class.java) ?: "UNKNOWN"

            // Parse payment details more robustly
            val paymentMethod = snapshot.child("paymentMethod").getValue(String::class.java) ?: "UNKNOWN"

            val paymentDetailsSnapshot = snapshot.child("paymentDetails")
            val paymentDetails = if (paymentDetailsSnapshot.exists()) {
                val paymentTime = paymentDetailsSnapshot.child("paymentTime").getValue(Long::class.java) ?: 0
                val transactionId = paymentDetailsSnapshot.child("transactionId").getValue(String::class.java) ?: ""
                PaymentDetails(paymentTime, transactionId, paymentMethod)
            } else {
                PaymentDetails(0, "", paymentMethod)
            }

            // Parse shipping address more robustly
            val shippingAddressSnapshot = snapshot.child("shippingAddress")
            val shippingAddress = if (shippingAddressSnapshot.exists()) {
                val address = shippingAddressSnapshot.child("address").getValue(String::class.java) ?: ""
                val city = shippingAddressSnapshot.child("city").getValue(String::class.java) ?: ""
                val state = shippingAddressSnapshot.child("state").getValue(String::class.java) ?: ""
                val zipCode = shippingAddressSnapshot.child("zipCode").getValue(String::class.java) ?: ""
                val fullName = shippingAddressSnapshot.child("fullName").getValue(String::class.java) ?: ""
                val phoneNumber = shippingAddressSnapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                val isDefault = shippingAddressSnapshot.child("isDefault").getValue(Boolean::class.java) ?: false

                ShippingAddress(
                    address = address,
                    city = city,
                    state = state,
                    zipCode = zipCode,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    isDefault = isDefault
                )
            } else {
                ShippingAddress("")
            }

            // Get order financial details
            val shipping = snapshot.child("shipping").getValue(Double::class.java) ?: 0.0
            val subtotal = snapshot.child("subtotal").getValue(Double::class.java) ?: 0.0
            val tax = snapshot.child("tax").getValue(Double::class.java) ?: 0.0
            val total = snapshot.child("total").getValue(Double::class.java) ?: 0.0
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0

            return Order(
                orderId = orderId,
                items = items,
                paymentDetails = paymentDetails,
                shippingAddress = shippingAddress,
                status = status,
                shipping = shipping,
                subtotal = subtotal,
                tax = tax,
                total = total,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e("OrderDetailViewModel", "Error parsing order: ${snapshot.key}", e)
            return null
        }
    }
}

data class OrderDetailUiState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)