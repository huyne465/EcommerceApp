package com.example.ecommerceapp.presentation.admin.manageProduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.ecommerceapp.model.Product

class ManageProductViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ManageProductUiState())
    val uiState: StateFlow<ManageProductUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val productsRef = database.getReference("products")

    fun loadProducts() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val snapshot = productsRef.get().await()

                val products = mutableListOf<Product>()
                snapshot.children.forEach { productSnapshot ->
                    val product = productSnapshot.getValue(Product::class.java)
                    if (product != null) {
                        product.id = productSnapshot.key ?: ""
                        products.add(product)
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        products = products
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load products: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true) }
                productsRef.child(productId).removeValue().await()

                // Remove product from the list
                _uiState.update { state ->
                    val updatedProducts = state.products.filter { it.id != productId }
                    state.copy(
                        isProcessing = false,
                        products = updatedProducts,
                        errorMessage = "Product deleted successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Failed to delete product: ${e.message}"
                    )
                }
            }
        }
    }
}

data class ManageProductUiState(
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null
)