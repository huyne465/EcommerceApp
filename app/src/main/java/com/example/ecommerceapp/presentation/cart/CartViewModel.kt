package com.example.ecommerceapp.presentation.cart


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ecommerceapp.model.CartItem
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

class CartViewModel : ViewModel() {

    // UI State for Cart screen
    data class CartUiState(
        val cartItems: List<CartItem> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val subtotal: Double = 0.0,
        val tax: Double = 0.0,
        val shipping: Double = 0.0,
        val total: Double = 0.0,
        val actionMessage: String? = null,
        val stock: Int = 0
    )

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://ecommerceapp-58b7f-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val cartRef = database.getReference("cart")

    // Get current user ID or generate anonymous ID if not signed in
    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"

    init {
        loadCartItems()
    }



    //validate stock
    fun validateStock(cartItem: CartItem): Boolean {
        val stock = _uiState.value.stock

        if (cartItem.quantity > stock) {
            _uiState.update {
                it.copy(
                    errorMessage = "Not enough stock available. Only $stock items left."
                )
            }
            return false
        } else {
            _uiState.update {
                it.copy(
                    errorMessage = null
                )
            }
            return true
        }
    }

    // Separate function to fetch current stock from Firebase
    fun refreshStockData(cartItem: CartItem, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val productRef = database.getReference("products").child(cartItem.productId)
                val stockSnapshot = productRef.child("stock").get().await()
                val stock = stockSnapshot.getValue(Int::class.java) ?: 0

                _uiState.update {
                    it.copy(stock = stock)
                }

                // Re-validate with new stock data
                validateStock(cartItem)

                // Call the completion handler if provided
                onComplete?.invoke(stock)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun updateQuantity(cartItem: CartItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeCartItem(cartItem)
            return
        }

        // First refresh stock data from server, then proceed with quantity update
        refreshStockData(cartItem) { currentStock ->
            if (newQuantity <= currentStock) {
                viewModelScope.launch {
                    try {
                        // Update quantity in Firebase
                        cartRef.child(userId).child(cartItem.id).child("quantity").setValue(newQuantity).await()

                        _uiState.update {
                            it.copy(
                                actionMessage = "Quantity updated"
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                actionMessage = "Failed to update quantity: ${e.message}"
                            )
                        }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = "Cannot add more items. Available stock is $currentStock."
                    )
                }
            }
        }
    }

     fun loadCartItems() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        cartRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cartItems = mutableListOf<CartItem>()
                for (itemSnapshot in snapshot.children) {
                    val cartItem = itemSnapshot.getValue(CartItem::class.java)
                    if (cartItem != null) {
                        // Ensure ID is set
                        cartItem.id = itemSnapshot.key ?: cartItem.id
                        cartItems.add(cartItem)
                    }
                }

                // Calculate costs
                val subtotal = cartItems.sumOf { it.price * it.quantity }
                val tax = subtotal * 0.1 // 10% tax
                val shipping = if (subtotal > 0) 3.0 else 0.0 // Shipping fee
                val total = subtotal + tax + shipping

                _uiState.update {
                    it.copy(
                        cartItems = cartItems.sortedByDescending { item -> item.timestamp },
                        isLoading = false,
                        errorMessage = null,
                        subtotal = subtotal,
                        tax = tax,
                        shipping = shipping,
                        total = total
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


    fun removeCartItem(cartItem: CartItem) {
        viewModelScope.launch {
            try {
                // Remove from Firebase
                cartRef.child(userId).child(cartItem.id).removeValue().await()

                _uiState.update {
                    it.copy(
                        actionMessage = "Item removed from cart"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        actionMessage = "Failed to remove item: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            try {
                // Remove all items from Firebase
                cartRef.child(userId).removeValue().await()

                _uiState.update {
                    it.copy(
                        actionMessage = "Cart cleared"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        actionMessage = "Failed to clear cart: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

}

// Factory for creating the ViewModel if you need parameters
class CartViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            return CartViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}