package com.example.ecommerceapp.presentation.revenueManage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecommerceapp.model.Order
import com.example.ecommerceapp.model.OrderItem
import com.example.ecommerceapp.model.PaymentDetails
import com.example.ecommerceapp.model.ShippingAddress
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RevenueManageViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RevenueManageUiState())
    val uiState: StateFlow<RevenueManageUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val ordersRef = database.getReference("orders")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Get all users' orders
                val snapshot = database.reference
                    .child("orders")
                    .get()
                    .await()

                val allOrders = mutableListOf<Order>()

                // First level: user IDs
                for (userSnapshot in snapshot.children) {
                    // Second level: order IDs
                    for (orderSnapshot in userSnapshot.children) {
                        // Only include PAID orders
                        val status = orderSnapshot.child("status").getValue(String::class.java)
                        if (status == "PAID") {
                            parseOrder(orderSnapshot)?.let {
                                allOrders.add(it)
                            }
                        }
                    }
                }

                // Apply date filter if dates are selected
                val filteredOrders = if (_uiState.value.startDate != null && _uiState.value.endDate != null) {
                    filterOrdersByDateRange(allOrders, _uiState.value.startDate!!, _uiState.value.endDate!!)
                } else {
                    allOrders
                }

                // Calculate total revenue
                val totalRevenue = filteredOrders.sumOf { it.total }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        orders = filteredOrders,
                        totalRevenue = totalRevenue
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load orders: ${e.message}"
                    )
                }
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
            return null
        }
    }

    fun setDateRange(startDate: Date, endDate: Date) {
        _uiState.update {
            it.copy(
                startDate = startDate,
                endDate = endDate
            )
        }
        loadOrders() // Reload with new date filter
    }


    fun clearDateFilter() {
        _uiState.update {
            it.copy(
                startDate = null,
                endDate = null
            )
        }
        loadOrders() // Reload without date filter
    }

    private fun filterOrdersByDateRange(orders: List<Order>, startDate: Date, endDate: Date): List<Order> {
        // Add one day to endDate to include orders from the entire end date
        val calendar = Calendar.getInstance()
        calendar.time = endDate
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val adjustedEndDate = calendar.time

        return orders.filter { order ->
            try {
                val orderDate = Date(order.timestamp)
                orderDate >= startDate && orderDate < adjustedEndDate
            } catch (e: Exception) {
                false // Skip orders with invalid dates
            }
        }
    }
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class RevenueManageUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val totalRevenue: Double = 0.0,
    val startDate: Date? = null,
    val endDate: Date? = null,
    val errorMessage: String? = null
)