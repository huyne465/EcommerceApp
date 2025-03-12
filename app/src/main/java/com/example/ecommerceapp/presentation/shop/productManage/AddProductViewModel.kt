package com.example.ecommerceapp.presentation.shop.productManage

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

class AddProductViewModel : ViewModel() {

    // UI State
    data class AddProductUiState(
        val name: String = "",
        val description: String = "",
        val price: String = "",
        val imageUrl: String = "",
        val brand: String = "",
        val category: String = "unknown",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val isSuccess: Boolean = false,
        val selectedCategory: String = ""
    )

    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val productsRef = database.getReference("products")

    // Event handling
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onSelectedCategoryChange(selectedCategory: String) {
        _uiState.update { it.copy(selectedCategory = selectedCategory) }
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

    // Individual field validation functions
    private fun validateName(): Boolean {
        if (_uiState.value.name.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Name must not be empty"
                )
            }
            return false
        }
        return true
    }

    private fun validateDescription(): Boolean {
        if (_uiState.value.description.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Description must not be empty"
                )
            }
            return false
        }
        return true
    }

    private fun validatePrice(): Boolean {
        if (_uiState.value.price.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Price must not be empty"
                )
            }
            return false
        }

        val priceValue = _uiState.value.price.toDoubleOrNull()
        if (priceValue == null) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Price must be a valid number"
                )
            }
            return false
        }

        if (priceValue <= 0) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Price must be greater than zero"
                )
            }
            return false
        }

        return true
    }

    private fun validateImageUrl(): Boolean {
        if (_uiState.value.imageUrl.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Image URL must not be empty"
                )
            }
            return false
        }
        return true
    }

    private fun validateBrand(): Boolean {
        if (_uiState.value.brand.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Brand must not be empty"
                )
            }
            return false
        }
        return true
    }

    private fun validateCategory(): Boolean {
        if (_uiState.value.selectedCategory.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Category must be selected"
                )
            }
            return false
        }
        return true
    }

    fun addProduct() {
        // Validate fields
        if (!validateName()) {
            return
        }

        if (!validateDescription()) {
            return
        }

        if (!validatePrice()) {
            return
        }

        if (!validateImageUrl()) {
            return
        }

        if (!validateBrand()) {
            return
        }

        if (!validateCategory()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val price = _uiState.value.price.toDouble()
                val productId = productsRef.push().key

                val product = Product(
                    id = productId!!,
                    name = _uiState.value.name,
                    description = _uiState.value.description,
                    price = price,
                    imageUrl = _uiState.value.imageUrl,
                    brand = _uiState.value.brand,
                    category = _uiState.value.selectedCategory,
                    rating = 0,
                    reviewCount = 0,
                    isFavorite = false
                )

                // Add to Firebase Realtime Database
                productsRef.child(productId).setValue(product).await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        // Reset form
                        name = "",
                        description = "",
                        price = "",
                        imageUrl = "",
                        brand = "",
                        category = "",
                        selectedCategory = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
}