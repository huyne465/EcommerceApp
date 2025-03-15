package com.example.ecommerceapp.presentation.manageOrder

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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ManageOrderUiState(
    val isLoading: Boolean = false,
    val pendingOrders: List<Order> = emptyList(),
    val errorMessage: String? = null
)

class ManageOrderViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")

    private val _uiState = MutableStateFlow(ManageOrderUiState())
    val uiState: StateFlow<ManageOrderUiState> = _uiState.asStateFlow()

    init {
        loadPendingOrders()
    }

    fun loadPendingOrders() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Get all orders from all users
                val snapshot = database.reference
                    .child("orders")
                    .get()
                    .await()

                val pendingOrders = mutableListOf<Order>()

                // First level: user IDs
                for (userSnapshot in snapshot.children) {
                    // Second level: order IDs
                    for (orderSnapshot in userSnapshot.children) {
                        // Only include PENDING orders
                        val status = orderSnapshot.child("status").getValue(String::class.java)
                        if (status == "PENDING") {
                            parseOrder(orderSnapshot)?.let {
                                pendingOrders.add(it)
                            }
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingOrders = pendingOrders
                    )
                }
            } catch (e: Exception) {
                Log.e("ManageOrderViewModel", "Error loading pending orders", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load pending orders: ${e.message}"
                    )
                }
            }
        }
    }

    fun confirmOrder(order: Order) {
        viewModelScope.launch {
            try {
                // Extract the userId from the parentPath
                val orderPath = database.reference
                    .child("orders")
                    .get()
                    .await()

                // Find the user ID that contains this order
                var userId: String? = null
                for (userSnapshot in orderPath.children) {
                    if (userSnapshot.child(order.orderId).exists()) {
                        userId = userSnapshot.key
                        break
                    }
                }

                if (userId == null) {
                    Log.e("ManageOrderViewModel", "Could not find user for order: ${order.orderId}")
                    _uiState.update {
                        it.copy(errorMessage = "Failed to confirm order: User not found")
                    }
                    return@launch
                }

                // Update adminConfirmed to true
                database.reference
                    .child("orders")
                    .child(userId)
                    .child(order.orderId)
                    .child("adminConfirmed")
                    .setValue(true)
                    .await()

                Log.d("ManageOrderViewModel", "Order ${order.orderId} confirmed by admin")

                // Reload pending orders
                loadPendingOrders()
            } catch (e: Exception) {
                Log.e("ManageOrderViewModel", "Error confirming order", e)
                _uiState.update {
                    it.copy(errorMessage = "Failed to confirm order: ${e.message}")
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun parseOrder(snapshot: DataSnapshot): Order? {
        try {
            val orderId = snapshot.key ?: return null

            val items = mutableListOf<OrderItem>()
            val itemsSnapshot = snapshot.child("items")
            for (itemSnapshot in itemsSnapshot.children) {
                val brand = itemSnapshot.child("brand").getValue(String::class.java) ?: "N/A"
                val imageUrl = itemSnapshot.child("imageUrl").getValue(String::class.java) ?: "N/A"
                val name = itemSnapshot.child("name").getValue(String::class.java) ?: ""
                val price = itemSnapshot.child("price").getValue(Double::class.java) ?: 0.0
                val productId = itemSnapshot.child("productId").getValue(String::class.java) ?: "N/A"
                val quantity = itemSnapshot.child("quantity").getValue(Int::class.java) ?: 0
                val selectedSize = itemSnapshot.child("selectedSize").getValue(String::class.java) ?: "N/A"

                items.add(OrderItem(brand, imageUrl, name, price, productId, quantity, selectedSize))
            }

            // Get status - this is mandatory
            val status = snapshot.child("status").getValue(String::class.java) ?: "UNKNOWN"


            // Handle payment details
            val paymentDetailsSnapshot = snapshot.child("paymentDetails")
            val paymentDetails = if (paymentDetailsSnapshot.exists()) {
                val paymentTime = paymentDetailsSnapshot.child("paymentTime").getValue(Long::class.java) ?: 0
                val transactionId = paymentDetailsSnapshot.child("transactionId").getValue(String::class.java) ?: ""
                val paymentMethod = paymentDetailsSnapshot.child("paymentMethod").getValue(String::class.java)
                    ?: snapshot.child("paymentMethod").getValue(String::class.java) ?: "N/A"
                PaymentDetails(paymentTime, transactionId, paymentMethod)
            } else {
                val paymentMethod = snapshot.child("paymentMethod").getValue(String::class.java) ?: "UNKNOWN"
                PaymentDetails(0, "", paymentMethod)
            }

            // Handle shipping address
            val shippingAddressSnapshot = snapshot.child("shippingAddress")
            val shippingAddress = if (shippingAddressSnapshot.exists()) {
                val address = shippingAddressSnapshot.child("address").getValue(String::class.java) ?: "N/A"
                val city = shippingAddressSnapshot.child("city").getValue(String::class.java) ?: "N/A"
                val fullName = shippingAddressSnapshot.child("fullName").getValue(String::class.java) ?: "N/A"
                ShippingAddress(
                    address = address,
                    city = city,
                    fullName = fullName
                )
            } else {
                ShippingAddress("")
            }

            // Get other order details
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
            Log.e("ManageOrderViewModel", "Error parsing order: ${snapshot.key}", e)
            return null
        }
    }
}

// Add userId field to PaymentDetails
data class PaymentDetails(
    val paymentTime: Long = 0,
    val transactionId: String = "",
    val paymentMethod: String = "",
    val userId: String = ""
)