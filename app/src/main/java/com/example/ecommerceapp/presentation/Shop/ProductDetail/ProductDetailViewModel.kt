package com.example.ecommerceapp.presentation.Shop.ProductDetail


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ecommerceapp.model.CartItem
import com.example.ecommerceapp.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProductDetailViewModel(private val productId: String) : ViewModel() {

    // UI State for Product Detail screen
    data class ProductDetailUiState(
        val product: Product? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val quantity: Int = 1,
        val actionMessage: String? = null
    )

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val productsRef = database.getReference("products")
    private val cartRef = database.getReference("cart")
    private val ratingsRef = database.getReference("ratings")

    // Get current user ID or generate anonymous ID if not signed in
    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"

    init {
        loadProduct()
    }

    fun loadProduct() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        productsRef.child(productId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val product = snapshot.getValue(Product::class.java)
                if (product != null) {
                    // Ensure ID is set
                    product.id = snapshot.key ?: productId
                    _uiState.update {
                        it.copy(
                            product = product,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Product not found"
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            }
        })
    }

    fun incrementQuantity() {
        _uiState.update { it.copy(quantity = it.quantity + 1) }
    }

    fun decrementQuantity() {
        if (_uiState.value.quantity > 1) {
            _uiState.update { it.copy(quantity = it.quantity - 1) }
        }
    }

    fun toggleFavorite() {
        val product = _uiState.value.product ?: return
        val newFavoriteStatus = !product.isFavorite

        // Reference to the favorites collection for this user
        val favoritesRef = database.getReference("favorites").child(userId)

        // Optimistically update UI first for responsive feel
        _uiState.update {
            it.copy(
                product = it.product?.copy(isFavorite = newFavoriteStatus)
            )
        }

        viewModelScope.launch {
            try {
                if (newFavoriteStatus) {
                    // Add complete product info to favorites node
                    favoritesRef.child(productId).setValue(product).await()

                    // Also update isFavorite status in products node
                    productsRef.child(productId).child("isFavorite").setValue(true).await()
                } else {
                    // Remove from favorites node
                    favoritesRef.child(productId).removeValue().await()

                    // Update isFavorite status in products node
                    productsRef.child(productId).child("isFavorite").setValue(false).await()
                }

                // Show success message
                _uiState.update {
                    it.copy(
                        actionMessage = if (newFavoriteStatus) "Added to favorites" else "Removed from favorites"
                    )
                }
            } catch (e: Exception) {
                // Revert the optimistic update on error
                _uiState.update {
                    it.copy(
                        product = it.product?.copy(isFavorite = !newFavoriteStatus),
                        actionMessage = "Failed to update favorite status: ${e.message}"
                    )
                }
            }
        }
    }

    fun submitRating(rating: Int) {
        val product = _uiState.value.product ?: return
        val currentRating = product.rating
        val currentReviewCount = product.reviewCount

        viewModelScope.launch {
            try {
                // Store user's rating
                ratingsRef.child(productId).child(userId).setValue(rating).await()

                // Calculate new average rating and update review count
                // This is a simplified approach - a more comprehensive solution would calculate
                // the average based on all ratings in the database
                val newReviewCount = currentReviewCount + 1
                val totalRatingPoints = (currentRating * currentReviewCount) + rating
                val newAverageRating = totalRatingPoints / newReviewCount

                // Update product rating in database
                val updates = hashMapOf<String, Any>(
                    "rating" to newAverageRating,
                    "reviewCount" to newReviewCount
                )

                productsRef.child(productId).updateChildren(updates).await()

                _uiState.update {
                    it.copy(
                        product = it.product?.copy(
                            rating = newAverageRating,
                            reviewCount = newReviewCount
                        ),
                        actionMessage = "Thank you for your rating!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        actionMessage = "Failed to submit rating: ${e.message}"
                    )
                }
            }
        }
    }

    fun addToCart() {
        val product = _uiState.value.product ?: return
        val quantity = _uiState.value.quantity

        viewModelScope.launch {
            try {
                // Create a cart item
                val cartItem = CartItem(
                    id = UUID.randomUUID().toString(),
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    quantity = quantity,
                    imageUrl = product.imageUrl,
                    brand = product.brand,
                    timestamp = System.currentTimeMillis()
                )

                // Add to user's cart in Firebase
                cartRef.child(userId).child(cartItem.id).setValue(cartItem).await()

                _uiState.update {
                    it.copy(
                        actionMessage = "${quantity} item(s) added to cart"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        actionMessage = "Failed to add to cart: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}

// Factory to create the ViewModel with productId parameter
class ProductDetailViewModelFactory(private val productId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductDetailViewModel::class.java)) {
            return ProductDetailViewModel(productId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}