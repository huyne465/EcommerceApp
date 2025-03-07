package com.example.ecommerceapp.model


data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val shipping: Double = 0.0,
    val total: Double = 0.0,
    val paymentMethod: String = "",
    val status: String = "",
    val timestamp: Long = 0
)