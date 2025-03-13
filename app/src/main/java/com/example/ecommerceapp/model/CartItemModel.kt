package com.example.ecommerceapp.model

data class CartItem(
    var id: String = "",
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val selectedSize: String? = null,
    val imageUrl: String = "",
    val brand: String = "",
    val timestamp: Long = 0,
    // For easy access in other screens
    var subtotal: Double = price * quantity
)