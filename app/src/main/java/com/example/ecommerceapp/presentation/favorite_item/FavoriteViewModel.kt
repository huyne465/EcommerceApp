package com.example.ecommerceapp.presentation.favorite_item


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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


class FavoriteViewModel : ViewModel() {
    data class FavoriteUiState(
        val favoriteProducts: List<Product> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(FavoriteUiState(isLoading = true))
    val uiState: StateFlow<FavoriteUiState> = _uiState.asStateFlow()

    private val database =
        FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val productsRef = database.getReference("products")
    private val favoritesRef = database.getReference("favorites")

    // Get current user ID or generate anonymous ID if not signed in
    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Listen to user's favorites list
        favoritesRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get all favorite product IDs
                val favoriteIds = snapshot.children.mapNotNull { it.key }

                if (favoriteIds.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            favoriteProducts = emptyList(),
                            isLoading = false
                        )
                    }
                    return
                }

                // Fetch the actual product data for those IDs
                productsRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(productsSnapshot: DataSnapshot) {
                        val favoriteProducts = mutableListOf<Product>()

                        for (productSnapshot in productsSnapshot.children) {
                            val productId = productSnapshot.key ?: continue

                            // Only add products that are in the user's favorites
                            if (productId in favoriteIds) {
                                val product = productSnapshot.getValue(Product::class.java)
                                product?.let {
                                    it.id = productId
                                    it.isFavorite = true // Ensure favorite status is set
                                    favoriteProducts.add(it)
                                }
                            }
                        }

                        _uiState.update {
                            it.copy(
                                favoriteProducts = favoriteProducts,
                                isLoading = false
                            )
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

    fun removeFavorite(productId: String) {
        viewModelScope.launch {
            try {
                // Remove from favorites collection
                favoritesRef.child(userId).child(productId).removeValue().await()

                // Update product's isFavorite status
                productsRef.child(productId).child("isFavorite").setValue(false).await()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Failed to remove favorite: ${e.message}"
                    )
                }
            }
        }
    }

}