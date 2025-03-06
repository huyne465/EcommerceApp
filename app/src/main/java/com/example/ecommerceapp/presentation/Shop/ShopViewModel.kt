package com.example.ecommerceapp.presentation.Shop

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

class ShopViewModel : ViewModel() {

    // Add a flag to track if we've already loaded products
    private var hasLoadedProducts = false

    // UI State for Shop screen
    data class ShopUiState(
        val products: List<Product> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val filterCategory: String? = null,
        val searchQuery: String = "",
        val sortOrder: SortOrder = SortOrder.NONE
    )

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

    init {
        loadProducts()
    }

    private var productsListener: ValueEventListener? = null

    fun loadProducts(forceReload: Boolean = false) {
        // Skip loading if we've already loaded products and not forcing reload
        if (hasLoadedProducts && !forceReload) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Remove any existing listener first
        productsListener?.let { productsRef.removeEventListener(it) }

        // Create and store the listener
        productsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productsList = mutableListOf<Product>()

                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let {
                        // Ensure ID is set from the key if not already present
                        if (it.id.isEmpty()) {
                            it.id = productSnapshot.key ?: ""
                        }
                        productsList.add(it)
                    }
                }

                _uiState.update {
                    it.copy(
                        products = productsList,
                        isLoading = false
                    )
                }

                // Mark that we've loaded products
                hasLoadedProducts = true
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            }
        }

        // Add the listener
        productsRef.addValueEventListener(productsListener!!)
    }

    // Don't forget to clean up in onCleared
    override fun onCleared() {
        super.onCleared()
        productsListener?.let { productsRef.removeEventListener(it) }
    }

    fun setFilterCategory(category: String?) {
        _uiState.update { it.copy(filterCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
    }

    fun resetSort() {
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

        // Apply search query filter - now only searching by name
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

        // Reference to the favorites collection for this user
        val favoritesRef = database.getReference("favorites").child(userId)

        // Optimistically update UI first for responsive feel
        val updatedProducts = productsList.map {
            if (it.id == productId) it.copy(isFavorite = newFavoriteStatus) else it
        }

        _uiState.update {
            it.copy(products = updatedProducts)
        }

        viewModelScope.launch {
            try {
                if (newFavoriteStatus) {
                    // Add complete product info to favorites node
                    favoritesRef.child(productId).setValue(productToUpdate.copy(isFavorite = true)).await()

                    // Also update isFavorite status in products node
                    productsRef.child(productId).child("favorite").setValue(true).await()
                } else {
                    // Remove from favorites node
                    favoritesRef.child(productId).removeValue().await()

                    // Update isFavorite status in products node
                    productsRef.child(productId).child("favorite").setValue(false).await()
                }
            } catch (e: Exception) {
                // Revert the optimistic update on error
                val revertedProducts = _uiState.value.products.map {
                    if (it.id == productId) it.copy(isFavorite = !newFavoriteStatus) else it
                    }

                    _uiState.update {
                        it.copy(products = revertedProducts)
                    }
                }
            }
        }
    }