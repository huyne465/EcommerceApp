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

class OrderHistoryViewModel : ViewModel() {
    // Match the same database instance used in OrderViewModel
    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(OrderHistoryUiState())
    val uiState: StateFlow<OrderHistoryUiState> = _uiState

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                Log.d("OrderHistoryViewModel", "Loading orders for user: $userId")

                // Navigate to orders > userId node
                val snapshot = database.reference
                    .child("orders")
                    .child(userId)
                    .get()
                    .await()

                Log.d("OrderHistoryViewModel", "Got snapshot with ${snapshot.childrenCount} orders")

                val orders = mutableListOf<Order>()
                for (orderSnapshot in snapshot.children) {
                    // Include all orders or filter as needed
                    // You might want to show all orders regardless of status
                    parseOrder(orderSnapshot)?.let {
                        orders.add(it)
                        Log.d("OrderHistoryViewModel", "Added order: ${orderSnapshot.key} with status: ${it.status}")
                    }
                }

                _uiState.value = OrderHistoryUiState(orders)
                Log.d("OrderHistoryViewModel", "Updated UI state with ${orders.size} orders")
            } catch (e: Exception) {
                Log.e("OrderHistoryViewModel", "Error loading orders", e)
                _uiState.value = OrderHistoryUiState(errorMessage = "Failed to load orders: ${e.message}")
            }
        }
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

            // Handle payment details more flexibly as they might not exist for all orders
            val paymentDetailsSnapshot = snapshot.child("paymentDetails")
            val paymentDetails = if (paymentDetailsSnapshot.exists()) {
                val paymentTime = paymentDetailsSnapshot.child("paymentTime").getValue(Long::class.java) ?: 0
                val transactionId = paymentDetailsSnapshot.child("transactionId").getValue(String::class.java) ?: ""
                // Try to get payment method from different locations since OrderViewModel stores it differently
                val paymentMethod = paymentDetailsSnapshot.child("paymentMethod").getValue(String::class.java)
                    ?: snapshot.child("paymentMethod").getValue(String::class.java) ?: "N/A"
                PaymentDetails(paymentTime, transactionId, paymentMethod)
            } else {
                // Fall back to getting just the payment method from the root node
                val paymentMethod = snapshot.child("paymentMethod").getValue(String::class.java) ?: "UNKNOWN"
                PaymentDetails(0, "", paymentMethod)
            }

            // Handle shipping address more flexibly
            val shippingAddressSnapshot = snapshot.child("shippingAddress")
            val shippingAddress = if (shippingAddressSnapshot.exists()) {
                val address = shippingAddressSnapshot.child("address").getValue(String::class.java) ?: "N/A"
                val city = shippingAddressSnapshot.child("city").getValue(String::class.java) ?: "N/A"
                val fullName = shippingAddressSnapshot.child("fullName").getValue(String::class.java) ?: "N/A"
                // Create a more complete shipping address object
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
            Log.e("OrderHistoryViewModel", "Error parsing order: ${snapshot.key}", e)
            return null
        }
    }
}

data class OrderHistoryUiState(
    val orders: List<Order> = emptyList(),
    val errorMessage: String? = null
)