package com.example.ecommerceapp.model

import com.example.ecommerceapp.presentation.Order.OrderViewModel
import com.example.ecommerceapp.presentation.profile.UserAddress.AddressList.AddressListViewModel


data class OrderUiState(
    val cartItems: List<CartItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val shipping: Double = 0.0,
    val total: Double = 0.0,
    val selectedPaymentMethod: OrderViewModel.PaymentMethod = OrderViewModel.PaymentMethod.COD,
    val actionMessage: String? = null,
    val orderPlaced: Boolean = false,
    val orderId: String? = null,
    val isProcessing: Boolean = false,
    val shippingAddress: AddressListViewModel.Address? = null,
    val zaloPayUrl: String? = null,
    val zaloPayOrderId: String? = null
)