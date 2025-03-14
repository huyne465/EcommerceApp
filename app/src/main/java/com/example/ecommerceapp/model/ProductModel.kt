package com.example.ecommerceapp.model


data class Product(
    var id: String = "",
    val name: String = "",
    val brand: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val selectedSize: String? = null,
    val rating: Int = 0,
    val reviewCount: Int = 0,
    val stock: Int = 0,
    val imageUrl: String = "",
    val category: String? = null,
    var isFavorite: Boolean = false,
)