package com.example.ecommerceapp.presentation.shop

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.lottie.animation.content.Content
import com.example.ecommerceapp.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import im.crisp.client.external.Crisp
import im.crisp.client.external.Crisp.configure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ShopViewModel : ViewModel() {

    // UI State for Shop screen
    data class ShopUiState(
        val products: List<Product> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val filterCategory: String? = null,
        val searchQuery: String = "",
        val sortOrder: SortOrder = SortOrder.NONE
    )

    // Add this companion object to store persistent state
    companion object {
        // Static variables to maintain state across instances
        private var persistentSortOrder: SortOrder = SortOrder.NONE
        private var persistentFilterCategory: String? = null
        private var persistentSearchQuery: String = ""
    }

    enum class SortOrder {
        NONE,
        NAME_A_TO_Z,
        NAME_Z_TO_A,
        PRICE_LOW_TO_HIGH,
        PRICE_HIGH_TO_LOW,
        RATING
    }


    private val _uiState = MutableStateFlow(ShopUiState(isLoading = true))
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val productsRef = database.getReference("products")
    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"

    private var productsListener: ValueEventListener? = null

    init {

        // Apply persisted settings on initialization
        _uiState.update { it.copy(
            sortOrder = persistentSortOrder,
            filterCategory = persistentFilterCategory,
            searchQuery = persistentSearchQuery,
            isLoading = true
        ) }
        // Force immediate load on initialization
        loadProducts(true)
    }


    // Crisp chat integration functions
    fun initializeCrisp(context: android.content.Context) {
        try {
            // Using ActivityCrisp utility class for configuration
            com.example.ecommerceapp.crispChatBox.ActivityCrisp.configureCrisp(context)
            Log.d("ShopViewModel", "Crisp chat initialized successfully")

            // Set user information if available
            FirebaseAuth.getInstance().currentUser?.let { user ->
                Crisp.setUserEmail(user.email ?: "")
                Crisp.setUserNickname(user.displayName ?: "")
            }
        } catch (e: Exception) {
            Log.e("ShopViewModel", "Failed to initialize Crisp: ${e.message}", e)
        }
    }

    fun openCrispChat(context: android.content.Context) {
        com.example.ecommerceapp.crispChatBox.ActivityCrisp.openCrispChat(context)
    }

    fun resetCrispChat(context: android.content.Context) {
        try {
            Crisp.resetChatSession(context)
            Log.d("ShopViewModel", "Crisp chat session reset successfully")
        } catch (e: Exception) {
            Log.e("ShopViewModel", "Failed to reset Crisp session: ${e.message}", e)
        }
    }

    fun loadProducts(forceReload: Boolean = false) {
        // Don't do anything if we're already loading
        if (_uiState.value.isLoading && !forceReload) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Remove previous listener if exists
        if (productsListener != null) {
            productsRef.removeEventListener(productsListener!!)
        }

        // Create new listener
        productsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productsList = mutableListOf<Product>()

                // Fetch user favorites to determine which products are favorited
                viewModelScope.launch {
                    try {
                        val favoritesSnapshot = database.getReference("favorites")
                            .child(userId)
                            .get()
                            .await()

                        val favoriteIds = favoritesSnapshot.children.mapNotNull { it.key }.toSet()

                        // Process products with favorite status
                        for (productSnapshot in snapshot.children) {
                            val product = productSnapshot.getValue(Product::class.java)
                            product?.let {
                                if (it.id.isEmpty()) {
                                    it.id = productSnapshot.key ?: ""
                                }
                                // Set favorite status
                                it.isFavorite = favoriteIds.contains(it.id)
                                productsList.add(it)
                            }
                        }

                        _uiState.update {
                            it.copy(
                                products = productsList,
                                isLoading = false
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Error loading products: ${e.message}"
                            )
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load products: ${error.message}"
                    )
                }
            }
        }

        // Attach the listener
        productsRef.addValueEventListener(productsListener!!)
    }

    // Override the onCleared method to clean up listeners
    override fun onCleared() {
        super.onCleared()
        productsListener?.let {
            productsRef.removeEventListener(it)
        }
    }

    fun setFilterCategory(category: String?) {
        persistentFilterCategory = category
        _uiState.update { it.copy(filterCategory = category) }
    }

    fun setSearchQuery(query: String) {
        persistentSearchQuery = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        persistentSortOrder = sortOrder
        _uiState.update { it.copy(sortOrder = sortOrder) }
    }

    fun resetSort() {
        persistentSortOrder = SortOrder.NONE
        _uiState.update { it.copy(sortOrder = SortOrder.NONE) }
    }

    fun getFilteredAndSortedProducts(): List<Product> {
        val currentState = _uiState.value
        var filteredProducts = currentState.products

        // Apply category filter
        currentState.filterCategory?.let { category ->
            if (category != "More" && category.isNotEmpty()) {
                filteredProducts = filteredProducts.filter {
                    it.category?.contains(category, ignoreCase = true) == true
                }
            }
        }

        // Apply search query filter
        if (currentState.searchQuery.isNotEmpty()) {
            val query = currentState.searchQuery.lowercase()
            filteredProducts = filteredProducts.filter {
                it.name.lowercase().contains(query)
            }
        }

        // Apply sorting
        filteredProducts = when (currentState.sortOrder) {
            SortOrder.NAME_A_TO_Z -> filteredProducts.sortedBy { it.name }
            SortOrder.NAME_Z_TO_A -> filteredProducts.sortedByDescending { it.name }
            SortOrder.PRICE_LOW_TO_HIGH -> filteredProducts.sortedBy { it.price }
            SortOrder.PRICE_HIGH_TO_LOW -> filteredProducts.sortedByDescending { it.price }
            SortOrder.RATING -> filteredProducts.sortedByDescending { it.rating }
            SortOrder.NONE -> filteredProducts
        }

        return filteredProducts
    }

    fun toggleFavorite(productId: String) {
        val productsList = _uiState.value.products
        val productToUpdate = productsList.find { it.id == productId } ?: return
        val newFavoriteStatus = !productToUpdate.isFavorite

        // Optimistically update UI first
        val updatedProducts = productsList.map {
            if (it.id == productId) it.copy(isFavorite = newFavoriteStatus) else it
        }

        _uiState.update {
            it.copy(products = updatedProducts)
        }

        // Reference to the favorites collection for this user
        val favoritesRef = database.getReference("favorites").child(userId)

        viewModelScope.launch {
            try {
                if (newFavoriteStatus) {
                    // Add to favorites
                    favoritesRef.child(productId).setValue(productToUpdate.copy(isFavorite = true)).await()
                } else {
                    // Remove from favorites
                    favoritesRef.child(productId).removeValue().await()
                }
                // No need to call loadProducts() here - the ValueEventListener will update if needed
            } catch (e: Exception) {
                // Revert optimistic update on error
                _uiState.update {
                    it.copy(products = it.products.map { product ->
                        if (product.id == productId) product.copy(isFavorite = !newFavoriteStatus) else product
                    })
                }
            }
        }
    }

}