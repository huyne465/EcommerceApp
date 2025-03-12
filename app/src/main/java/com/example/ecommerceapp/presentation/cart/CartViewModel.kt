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
        val actionMessage: String? = null
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

    private fun loadCartItems() {
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
                val shipping = if (subtotal > 0) 4.99 else 0.0 // Shipping fee
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

    fun updateQuantity(cartItem: CartItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeCartItem(cartItem)
            return
        }

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