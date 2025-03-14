package com.example.ecommerceapp.presentation.admin.manageProduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecommerceapp.model.Product
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProductViewModel : ViewModel() {

    data class EditProductUiState(
        val productId: String = "",
        val name: String = "",
        val description: String = "",
        val price: String = "",
        val imageUrl: String = "",
        val brand: String = "",
        val category: String? = null,
        val stock: String = "",
        val rating: Int = 0,
        val reviewCount: Int = 0,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val isSuccess: Boolean = false
    )

    private val _uiState = MutableStateFlow(EditProductUiState())
    val uiState: StateFlow<EditProductUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val productsRef = database.getReference("products")

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, productId = productId) }

            try {
                val snapshot = productsRef.child(productId).get().await()
                val product = snapshot.getValue(Product::class.java)

                if (product != null) {
                    _uiState.update {
                        it.copy(
                            name = product.name,
                            description = product.description,
                            price = product.price.toString(),
                            imageUrl = product.imageUrl,
                            brand = product.brand,
                            category = product.category,
                            stock = product.stock.toString(),
                            rating = product.rating,
                            reviewCount = product.reviewCount,
                            isLoading = false
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error loading product: ${e.message}"
                    )
                }
            }
        }
    }

    // Event handling
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onSelectedCategoryChange(selectedCategory: String) {
        _uiState.update { it.copy(category = selectedCategory) }
    }

    fun onPriceChange(price: String) {
        _uiState.update { it.copy(price = price) }
    }

    fun onImageUrlChange(imageUrl: String) {
        _uiState.update { it.copy(imageUrl = imageUrl) }
    }

    fun onBrandChange(brand: String) {
        _uiState.update { it.copy(brand = brand) }
    }

    fun onStockChange(stock: String) {
        // Only accept numeric input
        if (stock.isEmpty() || stock.all { it.isDigit() }) {
            _uiState.update { it.copy(stock = stock) }
        }
    }

    // Validation functions
    private fun validateName(): Boolean {
        if (_uiState.value.name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Name must not be empty") }
            return false
        }
        return true
    }

    private fun validateDescription(): Boolean {
        if (_uiState.value.description.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Description must not be empty") }
            return false
        }
        return true
    }

    private fun validatePrice(): Boolean {
        if (_uiState.value.price.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Price must not be empty") }
            return false
        }

        val priceValue = _uiState.value.price.toDoubleOrNull()
        if (priceValue == null) {
            _uiState.update { it.copy(errorMessage = "Price must be a valid number") }
            return false
        }

        if (priceValue <= 0) {
            _uiState.update { it.copy(errorMessage = "Price must be greater than zero") }
            return false
        }

        return true
    }

    private fun validateImageUrl(): Boolean {
        if (_uiState.value.imageUrl.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Image URL must not be empty") }
            return false
        }
        return true
    }

    private fun validateBrand(): Boolean {
        if (_uiState.value.brand.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Brand must not be empty") }
            return false
        }
        return true
    }

    private fun validateCategory(): Boolean {
        if (_uiState.value.category!!.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Category must be selected") }
            return false
        }
        return true
    }

    private fun validateStock(): Boolean {
        if (_uiState.value.stock.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Stock must not be empty") }
            return false
        }

        val stockValue = _uiState.value.stock.toIntOrNull()
        if (stockValue == null) {
            _uiState.update { it.copy(errorMessage = "Stock must be a valid number") }
            return false
        }

        if (stockValue < 0) {
            _uiState.update { it.copy(errorMessage = "Stock cannot be negative") }
            return false
        }

        return true
    }

    fun updateProduct() {
        if (!validateName() || !validateDescription() || !validatePrice() ||
            !validateImageUrl() || !validateBrand() || !validateCategory() || !validateStock()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val price = _uiState.value.price.toDouble()
                val stock = _uiState.value.stock.toInt()

                val updatedProduct = Product(
                    id = _uiState.value.productId,
                    name = _uiState.value.name,
                    description = _uiState.value.description,
                    price = price,
                    imageUrl = _uiState.value.imageUrl,
                    brand = _uiState.value.brand,
                    category = _uiState.value.category,
                    rating = _uiState.value.rating,
                    reviewCount = _uiState.value.reviewCount,
                    stock = stock,
                    isFavorite = false // We don't have this information when editing
                )

                // Update in Firebase
                productsRef.child(_uiState.value.productId).setValue(updatedProduct).await()

                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to update product: ${e.message}"
                    )
                }
            }
        }
    }
}