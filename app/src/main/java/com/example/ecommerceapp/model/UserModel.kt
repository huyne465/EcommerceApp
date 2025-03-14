package com.example.ecommerceapp.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserModel(
    val email: String = "",
    val name: String = "",
    val password: String = "",
    val confirmPassword: String ="",
    val createdAt: String = getCurrentFormattedDate(),
    var id: String = "",
    var photoUrl: String = "",
    val banned: Boolean = false
)


// Hàm lấy thời gian hiện tại dưới dạng chuỗi
fun getCurrentFormattedDate(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())  // Lấy thời gian hiện tại
}