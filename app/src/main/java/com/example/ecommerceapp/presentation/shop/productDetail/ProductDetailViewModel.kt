package com.example.ecommerceapp.presentation.shop.productDetail


import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ecommerceapp.model.CartItem
import com.example.ecommerceapp.model.MediaContent
import com.example.ecommerceapp.model.MediaType
import com.example.ecommerceapp.model.Product
import com.example.ecommerceapp.model.ProductComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID


class ProductDetailViewModel(application: Application,private val productId: String) : AndroidViewModel(application) {

    // UI State for Product Detail screen
    data class ProductDetailUiState(
        val product: Product? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val quantity: Int = 1,
        val actionMessage: String? = null,
        val comments: List<ProductComment> = emptyList(),
        val selectedSize: String? = null, // Add selected size
        val stock: Int = 0
    )

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val productsRef = database.getReference("products")
    private val cartRef = database.getReference("cart")
    private val ratingsRef = database.getReference("ratings")
    private val commentsRef = database.getReference("comments")

    // Check if user has already reviewed this product
    private var _hasUserReviewed = MutableStateFlow(false)
    val hasUserReviewed: StateFlow<Boolean> = _hasUserReviewed

    // Get current user information
    private val auth = FirebaseAuth.getInstance()
    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"

    init {
        loadProduct()
        loadComments()
        checkIfUserHasReviewed()
    }

    private fun checkIfUserHasReviewed() {
        viewModelScope.launch {
            try {
                val reviewSnapshot = commentsRef.child(productId).child(userId).get().await()
                _hasUserReviewed.value = reviewSnapshot.exists()
            } catch (e: Exception) {
                Log.e("ProductDetailViewModel", "Error checking user review: ${e.message}")
            }
        }
    }

    fun validatePickedSize(): Boolean {
        val selectedSize = _uiState.value.selectedSize
        if (selectedSize == null) {
            _uiState.update {
                it.copy(errorMessage = "Please select a size")
            }
            return false
        }
        return true
    }

    fun selectSize(size: String) {
        _uiState.update { it.copy(selectedSize = size) }
    }


    fun validateStock(): Boolean {
        val product = _uiState.value.product ?: return false
        val quantity = _uiState.value.quantity
        val stock = _uiState.value.stock

        // Direct comparison with current known stock value
        if (quantity > stock) {
            _uiState.update {
                it.copy(
                    errorMessage = "Not enough stock available. Only $stock items left."
                )
            }
            return false
        } else {
            _uiState.update {
                it.copy(errorMessage = null)
            }
            return true
        }
    }

    // Separate function to fetch current stock from Firebase
    fun refreshStockData(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val stockSnapshot = productsRef.child(productId).child("stock").get().await()
                val stock = stockSnapshot.getValue(Int::class.java) ?: 0

                _uiState.update {
                    it.copy(stock = stock)
                }

                // Re-validate with new stock data
                validateStock()

                // Call the completion handler if provided
                onComplete?.invoke(stock)
            } catch (e: Exception) {
                Log.e("ProductDetailViewModel", "Error refreshing stock: ${e.message}")
            }
        }
    }

    fun incrementQuantity() {
        // First refresh stock data from server, then proceed with quantity update
        refreshStockData { currentStock ->
            val currentQuantity = _uiState.value.quantity

            if (currentQuantity < currentStock) {
                _uiState.update { it.copy(quantity = it.quantity + 1) }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Cannot add more items. Available stock is $currentStock.")
                }
            }
        }
    }

    // Function to load comments for this product
    private fun loadComments() {
        commentsRef.child(productId).orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val commentsList = mutableListOf<ProductComment>()
                for (childSnapshot in snapshot.children) {
                    val comment = childSnapshot.getValue(ProductComment::class.java)
                    comment?.let { commentsList.add(it) }
                }

                // Sort comments by timestamp descending (newest first)
                commentsList.sortByDescending { it.timestamp }

                _uiState.update { it.copy(comments = commentsList) }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProductDetailViewModel", "Error loading comments: ${error.message}")
            }
        })
    }

    fun submitRatingWithComment(rating: Int, comment: String, mediaUris: List<Uri>) {
        val product = _uiState.value.product ?: return

        // If user already reviewed, don't allow another review
        if (_hasUserReviewed.value) {
            _uiState.update {
                it.copy(actionMessage = "You've already reviewed this product")
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(actionMessage = "Processing your review...") }

                val userProfileUrl = getUserProfileImageUrl()
                val displayName = getUserName()

                // Convert images to Base64
                val mediaContents = convertMediaUrisToBase64(mediaUris)

                // Create comment object with media content
                val productComment = ProductComment(
                    userId = userId,
                    username = displayName,
                    userProfileUrl = userProfileUrl,
                    rating = rating,
                    comment = comment,
                    mediaUrls = mediaContents,
                    timestamp = System.currentTimeMillis()
                )

                // Store the rating and comment
                ratingsRef.child(productId).child(userId).setValue(rating).await()
                commentsRef.child(productId).child(userId).setValue(productComment).await()

                // Update product rating
                updateProductRating(product, rating)

                // Mark that user has reviewed
                _hasUserReviewed.value = true

                _uiState.update {
                    it.copy(actionMessage = "Review submitted successfully")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(actionMessage = "Failed to submit review: ${e.message}")
                }
            }
        }
    }

    // Helper function to convert media URIs to Base64 strings
    private suspend fun convertMediaUrisToBase64(uris: List<Uri>): List<MediaContent> {
        return withContext(Dispatchers.IO) {
            uris.take(3).mapNotNull { uri ->
                try {
                    val contentResolver = getApplication<Application>().contentResolver
                    val inputStream = contentResolver.openInputStream(uri) ?: return@mapNotNull null
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    // Resize bitmap to reduce storage size
                    val resizedBitmap = resizeBitmap(bitmap, 800) // Max width 800px

                    // Convert to Base64
                    val base64String = convertBitmapToBase64(resizedBitmap)

                    // Determine media type
                    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                    val isVideo = mimeType.startsWith("video/")

                    MediaContent(
                        url = base64String,
                        type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                        thumbnailUrl = if (isVideo) base64String else ""
                    )
                } catch (e: Exception) {
                    Log.e("ProductDetailViewModel", "Error converting media: ${e.message}")
                    null
                }
            }
        }
    }

    // Helper function to resize bitmap
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth) {
            return bitmap
        }

        val aspectRatio = width.toFloat() / height.toFloat()
        val newHeight = (maxWidth / aspectRatio).toInt()

        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    // Helper function to convert bitmap to base64
    fun convertBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to JPEG with 70% quality to reduce size
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
    }

    // Function to update product rating
    private suspend fun updateProductRating(product: Product, newRating: Int) {
        try {
            // Get all ratings for this product
            val ratingsSnapshot = ratingsRef.child(productId).get().await()
            var totalRating = 0
            var ratingCount = 0

            // Calculate average rating
            for (ratingSnapshot in ratingsSnapshot.children) {
                val rating = ratingSnapshot.getValue(Int::class.java)
                if (rating != null) {
                    totalRating += rating
                    ratingCount++
                }
            }

            // Calculate new average
            val averageRating = if (ratingCount > 0) {
                totalRating.toFloat() / ratingCount
            } else {
                newRating.toFloat()
            }

            // Update product with new rating
            productsRef.child(productId).child("rating").setValue(averageRating).await()
            productsRef.child(productId).child("reviewCount").setValue(ratingCount).await()

            // Update local product object
            _uiState.update {
                it.copy(
                    product = it.product?.copy(
                        rating = averageRating.toInt(),
                        reviewCount = ratingCount
                    ),
                    actionMessage = "Review submitted successfully"
                )
            }
        } catch (e: Exception) {
            Log.e("ProductDetailViewModel", "Error updating product rating: ${e.message}")
        }
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
                    productsRef.child(productId).child("favorite").setValue(true).await()
                } else {
                    // Remove from favorites node
                    favoritesRef.child(productId).removeValue().await()

                    // Update isFavorite status in products node
                    productsRef.child(productId).child("favorite").setValue(false).await()
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

    fun addToCart() {
        if (!validatePickedSize()) {
            return
        }
        val product = _uiState.value.product ?: return
        val quantity = _uiState.value.quantity
        val selectedSize = _uiState.value.selectedSize ?: return


        viewModelScope.launch {
            try {
                // Create a cart item
                val cartItem = CartItem(
                    id = UUID.randomUUID().toString(),
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    selectedSize = selectedSize,
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


    private suspend fun getUserName(): String {
        val currentUser = auth.currentUser
        return try {
            val userSnapshot = database.getReference("users")
                .child(userId).get().await()
            userSnapshot.child("name").getValue(String::class.java)
                ?: currentUser?.displayName
                ?: "User 101"
        } catch (e: Exception) {
            Log.e("ProductDetailViewModel", "Error getting username: ${e.message}")
            currentUser?.displayName ?: "User 101"
        }
    }

    private suspend fun getUserProfileImageUrl(): String {
        return try {
            val userSnapshot = database.getReference("users")
                .child(userId).get().await()
            userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
        } catch (e: Exception) {
            Log.e("ProductDetailViewModel", "Error getting profile image: ${e.message}")
            auth.currentUser?.photoUrl?.toString() ?: ""
        }
    }
}

// Factory to create the ViewModel with productId parameter
class ProductDetailViewModelFactory(private val application: Application, private val productId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductDetailViewModel::class.java)) {
            return ProductDetailViewModel(application, productId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}