package com.example.ecommerceapp.presentation.Order

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecommerceapp.model.CartItem
import com.example.ecommerceapp.presentation.profile.UserAddress.AddressList.AddressListViewModel
import com.example.ecommerceapp.zalopay.Api.CreateOrder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import vn.zalopay.sdk.ZaloPayError
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener
import java.util.Date
import java.util.UUID

class OrderViewModel : ViewModel() {

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
        val shippingAddress: AddressListViewModel.Address? = null,
        val isZaloPayRequested: Boolean = false,
        val zaloPayToken: String? = null,
        val zaloPayAppTransID: String? = null
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

                val subtotal = cartItems.sumOf { it.price * it.quantity }
                val tax = subtotal * 0.1
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
        val tax = subtotal * 0.1
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

    private suspend fun getAddressById(addressId: String): AddressListViewModel.Address? {
        return try {
            val addressSnapshot = database.getReference("users").child(userId).child("addresses").child(addressId).get().await()
            if (addressSnapshot.exists()) {
                val address = addressSnapshot.getValue(AddressListViewModel.Address::class.java)
                // Make sure to set the id field as it might not be included in the serialized data
                address?.id = addressId
                address
            } else {
                null
            }
        } catch (e: Exception) {
            println("DEBUG: Failed to load address: ${e.message}")
            e.printStackTrace()
            null
        }
    }


    private fun processZaloPayment(activity: Activity, orderId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val amountInt = amount.toInt().toString() // ZaloPay expects amount as string

                // Create ZaloPay order using the API
                val createOrder = CreateOrder()
                val response = withContext(Dispatchers.IO) {
                    createOrder.createOrder(amountInt)
                }

                if (response != null) {
                    val returnCode = response.getInt("return_code")
                    val returnMessage = response.getString("return_message")

                    if (returnCode == 1) {
                        // Success - get the payment token
                        val zpTransToken = response.getString("zp_trans_token")

                        // Check if the app_trans_id exists and handle it properly
                        val appTransID = if (response.has("app_trans_id")) {
                            response.getString("app_trans_id")
                        } else {
                            // If not found, log the issue and use an alternative or generate one
                            println("DEBUG: app_trans_id not found in ZaloPay response")
                            "app_trans_id_" + UUID.randomUUID().toString() // Fallback ID
                        }

                        // Log the response for debugging
                        println("DEBUG: ZaloPay response: $response")

                        // Update state with token and transaction ID
                        _uiState.update {
                            it.copy(
                                isZaloPayRequested = true,
                                zaloPayToken = zpTransToken,
                                zaloPayAppTransID = appTransID
                            )
                        }

                        // Initiate payment with ZaloPay SDK
                        ZaloPaySDK.getInstance().payOrder(activity, zpTransToken, "demozpdk://app", object : PayOrderListener {
                            override fun onPaymentCanceled(zpTransToken: String?, appTransID: String?) {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isProcessing = false,
                                        actionMessage = "Payment was canceled",
                                        isZaloPayRequested = false
                                    )
                                }
                            }

                            override fun onPaymentError(zaloPayError: ZaloPayError?, zpTransToken: String?, appTransID: String?) {
                                val errorMessage = when (zaloPayError) {
                                    ZaloPayError.PAYMENT_APP_NOT_FOUND -> "ZaloPay app not found"
                                    else -> "Payment failed: ${zaloPayError?.name}"
                                }

                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isProcessing = false,
                                        errorMessage = errorMessage,
                                        isZaloPayRequested = false
                                    )
                                }
                            }

                            override fun onPaymentSucceeded(transactionId: String, transToken: String, appTransID: String?) {
                                // Update order status in Firebase
                                viewModelScope.launch {
                                    try {
                                        ordersRef.child(userId).child(orderId).child("status").setValue("PAID").await()
                                        ordersRef.child(userId).child(orderId).child("paymentDetails").setValue(mapOf(
                                            "transactionId" to transactionId,
                                            "paymentTime" to Date().time
                                        )).await()

                                        _uiState.update {
                                            it.copy(
                                                isLoading = false,
                                                isProcessing = false,
                                                orderPlaced = true,
                                                isZaloPayRequested = false,
                                                actionMessage = "Payment successful! Your order has been placed."
                                            )
                                        }
                                    } catch (e: Exception) {
                                        _uiState.update {
                                            it.copy(
                                                isLoading = false,
                                                isProcessing = false,
                                                isZaloPayRequested = false,
                                                errorMessage = "Payment was successful but failed to update order: ${e.message}"
                                            )
                                        }
                                    }
                                }
                            }
                        })
                    } else {
                        // Handle error
                        _uiState.update {
                            it.copy(
                                errorMessage = "ZaloPay Error: $returnMessage",
                                isLoading = false,
                                isProcessing = false
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to create ZaloPay order",
                            isLoading = false,
                            isProcessing = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "ZaloPay Error: ${e.message}",
                        isLoading = false,
                        isProcessing = false
                    )
                }
            }
        }
    }

    fun placeOrder(addressId: String) {
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
                // Retrieve the address
                val address = getAddressById(addressId)
                if (address == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isProcessing = false,
                            actionMessage = "Failed to retrieve address"
                        )
                    }
                    return@launch
                }

                // Create new order
                val orderId = UUID.randomUUID().toString()

                // Create order data as a Map
                val orderData = mapOf(
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
                    "shippingAddress" to mapOf(
                        "fullName" to address.fullName,
                        "phoneNumber" to address.phoneNumber,
                        "address" to address.address,
                        "city" to address.city,
                        "state" to address.state,
                        "zipCode" to address.zipCode,
                        "isDefault" to address.isDefault
                    ),
                    "timestamp" to Date().time
                )

                // Save order to Firebase
                ordersRef.child(userId).child(orderId).setValue(orderData).await()

                // Clear the cart
                cartRef.child(userId).removeValue().await()

                // For COD and other non-ZaloPay methods, complete the order immediately
                if (currentState.selectedPaymentMethod != PaymentMethod.ZALO_PAY) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isProcessing = false,
                            orderPlaced = true,
                            orderId = orderId,
                            actionMessage = "Order placed successfully!"
                        )
                    }
                } else {
                    // For ZaloPay, we'll keep processing state on until ZaloPay is initialized
                    _uiState.update {
                        it.copy(
                            orderId = orderId
                        )
                    }
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

    // This function will be called from the UI when we have the Activity reference
    fun initiateZaloPayPayment(activity: Activity) {
        val currentState = _uiState.value
        val orderId = currentState.orderId ?: return

        processZaloPayment(activity, orderId, currentState.total)
    }



    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}