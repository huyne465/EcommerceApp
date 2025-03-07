package com.example.ecommerceapp.presentation.Order

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecommerceapp.model.CartItem
import com.example.ecommerceapp.model.Order
import com.example.ecommerceapp.presentation.profile.UserAddress.AddressList.AddressListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class OrderViewModel() : ViewModel() {

    enum class PaymentMethod {
        COD, ZALO_PAY, GOOGLE_PAY;

        val displayName: String
            get() = when(this) {
                COD -> "Cash On Delivery"
                ZALO_PAY -> "ZaloPay"
                GOOGLE_PAY -> "Google Pay"
            }
    }

    data class OrderUiState(
        val cartItems: List<CartItem> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val subtotal: Double = 0.0,
        val tax: Double = 0.0,
        val shipping: Double = 0.0,
        val total: Double = 0.0,
        val selectedPaymentMethod: PaymentMethod = PaymentMethod.COD,
        val actionMessage: String? = null,
        val orderPlaced: Boolean = false,
        val orderId: String? = null,
        val isProcessing: Boolean = false,
    )

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val cartRef = database.getReference("cart")
    private val ordersRef = database.getReference("orders")

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"

    init {
        loadCartItems()
    }

    fun loadCartItems(forceReload: Boolean = false) {
        // Don't reload if already loading and not forced
        if (_uiState.value.isLoading && !forceReload) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val snapshot = cartRef.child(userId).get().await()
                val cartItems = mutableListOf<CartItem>()

                for (itemSnapshot in snapshot.children) {
                    val cartItem = itemSnapshot.getValue(CartItem::class.java)
                    if (cartItem != null) {
                        cartItem.id = itemSnapshot.key ?: cartItem.id
                        cartItems.add(cartItem)
                    }
                }

                // Calculate costs
                val subtotal = cartItems.sumOf { it.price * it.quantity }
                val tax = subtotal * 0.1 // 10% tax
                val shipping = if (subtotal > 0) 4.99 else 0.0
                val total = subtotal + tax + shipping

                _uiState.update {
                    it.copy(
                        cartItems = cartItems,
                        isLoading = false,
                        subtotal = subtotal,
                        tax = tax,
                        shipping = shipping,
                        total = total
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load cart items: ${e.message}"
                    )
                }
            }
        }
    }

    fun recalculateOrderTotals() {
        val cartItems = _uiState.value.cartItems
        val subtotal = cartItems.sumOf { it.price * it.quantity }
        val tax = subtotal * 0.1 // 10% tax
        val shipping = if (subtotal > 0) 4.99 else 0.0
        val total = subtotal + tax + shipping

        _uiState.update {
            it.copy(
                subtotal = subtotal,
                tax = tax,
                shipping = shipping,
                total = total
            )
        }
    }


    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update {
            it.copy(selectedPaymentMethod = method)
        }
    }


    fun placeOrder() {
        val currentState = _uiState.value


        if (currentState.cartItems.isEmpty()) {
            _uiState.update {
                it.copy(actionMessage = "Your cart is empty")
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, isProcessing = true) }

        viewModelScope.launch {
            try {
                // Create new order
                val orderId = UUID.randomUUID().toString()

                // Create order data as a Map
                val orderData = mapOf(
                    "id" to orderId,
                    "userId" to userId,
                    "items" to currentState.cartItems.map { item ->
                        mapOf(
                            "productId" to item.productId,
                            "name" to item.name,
                            "brand" to item.brand,
                            "price" to item.price,
                            "quantity" to item.quantity,
                            "imageUrl" to item.imageUrl
                        )
                    },
                    "subtotal" to currentState.subtotal,
                    "tax" to currentState.tax,
                    "shipping" to currentState.shipping,
                    "total" to currentState.total,
                    "paymentMethod" to currentState.selectedPaymentMethod.name,
                    "status" to "PENDING",
                    "timestamp" to Date().time
                )

                // Save order to Firebase using the map
                ordersRef.child(userId).child(orderId).setValue(orderData).await()

                // Clear the cart
                cartRef.child(userId).removeValue().await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isProcessing = false,
                        orderPlaced = true,
                        orderId = orderId,
                        actionMessage = "Order placed successfully!"
                    )
                }

                println("DEBUG: Order saved successfully with ID: $orderId")

            } catch (e: Exception) {
                println("DEBUG: Failed to save order: ${e.message}")
                e.printStackTrace()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isProcessing = false,
                        errorMessage = "Failed to place order: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

}