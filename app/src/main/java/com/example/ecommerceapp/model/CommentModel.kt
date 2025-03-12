package com.example.ecommerceapp.model

data class ProductComment(
    val userId: String = "",
    val username: String = "Anonymous",
    val userProfileUrl: String = "", // Add this field for profile image URL
    val rating: Int = 0,
    val comment: String = "",
    val mediaUrls: List<MediaContent> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class MediaContent(
    val url: String = "",
    val type: MediaType = MediaType.IMAGE,
    val thumbnailUrl: String = ""  // For videos
)

enum class MediaType {
    IMAGE, VIDEO
}