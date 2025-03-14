package com.example.ecommerceapp.model

data class Order(
    var orderId: String,
    val items: List<OrderItem>,
    val paymentDetails: PaymentDetails,
    val shippingAddress: ShippingAddress,
    val status: String,
    val shipping: Double,
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double,
    val timestamp: Long = 0
)

data class OrderItem(
    val brand: String,
    val imageUrl: String,
    val name: String,
    val price: Double,
    val productId: String,
    val quantity: Int,
    val selectedSize: String
)

data class PaymentDetails(
    val paymentTime: Long,
    val transactionId: String,
    val paymentMethod: String
)

data class ShippingAddress(
    val address: String,
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val isDefault: Boolean = false
)
