package com.example.ecommerceapp.presentation.profile.reviews

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.ecommerceapp.model.Product
import com.example.ecommerceapp.model.ProductComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UserReviewsViewModel : ViewModel() {

    data class UserReviewItem(
        val productId: String = "",
        val productName: String = "",
        val productImageUrl: String = "",
        val productBrand: String = "",
        val comment: ProductComment = ProductComment()
    )

    data class UserReviewsUiState(
        val reviews: List<UserReviewItem> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UserReviewsUiState())
    val uiState: StateFlow<UserReviewsUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()
    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous"

    init {
        loadUserReviews()
    }

    private fun loadUserReviews() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Reference to comments for all products
        val commentsRef = database.getReference("comments")
        val productsRef = database.getReference("products")

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userReviews = mutableListOf<UserReviewItem>()

                // First pass: collect all product IDs where user has commented
                val productIds = mutableListOf<String>()
                for (productSnapshot in snapshot.children) {
                    val productId = productSnapshot.key ?: continue

                    // Check if user has review for this product
                    val userReviewSnapshot = productSnapshot.child(userId)
                    if (userReviewSnapshot.exists()) {
                        productIds.add(productId)
                    }
                }

                // If no reviews, update state and return
                if (productIds.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            reviews = emptyList(),
                            isLoading = false
                        )
                    }
                    return
                }

                // Counter to track when all products are processed
                var productsProcessed = 0

                // Second pass: get product details and create UserReviewItem objects
                for (productId in productIds) {
                    // Get the user's comment for this product
                    val userComment = snapshot.child(productId).child(userId)
                        .getValue(ProductComment::class.java) ?: continue

                    // Get product details
                    productsRef.child(productId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(productSnapshot: DataSnapshot) {
                            val product = productSnapshot.getValue(Product::class.java)
                            if (product != null) {
                                // Create UserReviewItem
                                val reviewItem = UserReviewItem(
                                    productId = productId,
                                    productName = product.name,
                                    productImageUrl = product.imageUrl,
                                    productBrand = product.brand,
                                    comment = userComment
                                )

                                userReviews.add(reviewItem)
                            }

                            // Increment counter
                            productsProcessed++

                            // If all products processed, update UI state
                            if (productsProcessed == productIds.size) {
                                // Sort reviews by timestamp (newest first)
                                userReviews.sortByDescending { it.comment.timestamp }

                                _uiState.update {
                                    it.copy(
                                        reviews = userReviews,
                                        isLoading = false
                                    )
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            productsProcessed++

                            // If all products processed, update UI state
                            if (productsProcessed == productIds.size) {
                                _uiState.update {
                                    it.copy(
                                        reviews = userReviews,
                                        isLoading = false
                                    )
                                }
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserReviewsViewModel", "Error loading reviews: ${error.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load reviews: ${error.message}"
                    )
                }
            }
        })
    }
}