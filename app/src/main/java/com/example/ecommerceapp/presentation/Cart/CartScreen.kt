package com.example.ecommerceapp.presentation.Cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.ecommerceapp.model.CartItem
import com.example.ecommerceapp.presentation.profile.UserAddress.AddressList.AddressListViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CartViewModel = viewModel(),
    addressListViewModel: AddressListViewModel = viewModel(),
    navController: NavHostController,
    onNavigateToHome: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToProduct: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearCartConfirmation by remember { mutableStateOf(false) }
    val addressUiState by addressListViewModel.uiState.collectAsState()
    var showAddAddressDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        addressListViewModel.loadAddresses()
    }


    LaunchedEffect(addressUiState.addresses, addressUiState.isLoading) {
        if (addressUiState.isLoading) {
            // Don't show dialog while loading
            showAddAddressDialog = false
        } else if (addressUiState.addresses.isEmpty()) {
            showAddAddressDialog = true
        } else {
            showAddAddressDialog = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Shopping Cart") },
                actions = {
                    if (uiState.cartItems.isNotEmpty()) {
                        IconButton(onClick = { showClearCartConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Cart")
                        }
                    }
                }
            )
        },
        bottomBar = {
            val selectedColor = Color.Gray
            val unSelectedColor = Color.DarkGray
            NavigationBar(containerColor = Color(0xFFF0F0F0)) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = false,
                    onClick = onNavigateToHome,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unSelectedColor,
                        unselectedTextColor = unSelectedColor,
                        indicatorColor = Color.Black
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Shop") },
                    label = { Text("Shop") },
                    selected = false,
                    onClick = onNavigateToShop,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unSelectedColor,
                        unselectedTextColor = unSelectedColor,
                        indicatorColor = Color.Black
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Bag") },
                    label = { Text("Bag") },
                    selected = true,
                    onClick = { /* Already on Cart */ },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unSelectedColor,
                        unselectedTextColor = unSelectedColor,
                        indicatorColor = Color.Black
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                    label = { Text("Favorites") },
                    selected = false,
                    onClick = onNavigateToFavorites,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unSelectedColor,
                        unselectedTextColor = unSelectedColor,
                        indicatorColor = Color.Black
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = onNavigateToProfile,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unSelectedColor,
                        unselectedTextColor = unSelectedColor,
                        indicatorColor = Color.Black
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (uiState.cartItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your cart is empty",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Browse products and add items to your cart",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToShop,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continue Shopping")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Display default address if available
                    addressUiState.addresses.find { it.isDefault }?.let { defaultAddress ->
                        Row {
                            Text(
                                text = "Shipping to: ",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${defaultAddress.fullName}, ${defaultAddress.address}, ${defaultAddress.city}, ${defaultAddress.state}, ${defaultAddress.zipCode}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    // Cart items list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.cartItems) { cartItem ->
                            CartItemCard(
                                cartItem = cartItem,
                                onQuantityIncrease = {
                                    viewModel.updateQuantity(
                                        cartItem,
                                        cartItem.quantity + 1
                                    )
                                },
                                onQuantityDecrease = {
                                    viewModel.updateQuantity(
                                        cartItem,
                                        cartItem.quantity - 1
                                    )
                                },
                                onRemove = { viewModel.removeCartItem(cartItem) },
                                onItemClick = { onNavigateToProduct(cartItem.productId) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Order summary
                    OrderSummary(
                        subtotal = uiState.subtotal,
                        tax = uiState.tax,
                        shipping = uiState.shipping,
                        total = uiState.total
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checkout button
                    Button(
                        onClick = { navController.navigate("order") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Proceed to Checkout",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            if (uiState.actionMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Text(text = uiState.actionMessage ?: "")
                }
                LaunchedEffect(uiState.actionMessage) {
                    viewModel.clearActionMessage()
                }
            }

            if (showClearCartConfirmation) {
                AlertDialog(
                    onDismissRequest = { showClearCartConfirmation = false },
                    title = { Text("Clear Cart") },
                    text = { Text("Are you sure you want to remove all items from your cart?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearCart()
                                showClearCartConfirmation = false
                            }
                        ) {
                            Text("Clear")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showClearCartConfirmation = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showAddAddressDialog) {
                AlertDialog(
                    onDismissRequest = { showAddAddressDialog = false },
                    title = { Text("No Address Found") },
                    text = { Text("You don't have any saved addresses. Would you like to add one now?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showAddAddressDialog = false
                                navController.navigate("add_address")
                            }
                        ) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showAddAddressDialog = false }) {
                            Text("No")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    onQuantityIncrease: () -> Unit,
    onQuantityDecrease: () -> Unit,
    onRemove: () -> Unit,
    onItemClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clickable(onClick = onItemClick),
            horizontalArrangement = Arrangement.Start
        ) {
            // Product Image
            AsyncImage(
                model = cartItem.imageUrl,
                contentDescription = cartItem.name,
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            // Product Details
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = cartItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Text(
                    text = cartItem.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                // Price
                Text(
                    text = formatPrice(cartItem.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Quantity Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onQuantityDecrease,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease quantity",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = cartItem.quantity.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = onQuantityIncrease,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase quantity",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Remove Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.Top)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove item",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun OrderSummary(
    subtotal: Double,
    tax: Double,
    shipping: Double,
    total: Double,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Order Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Subtotal",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatPrice(subtotal),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tax",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatPrice(tax),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Shipping",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatPrice(shipping),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatPrice(total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Utility function to format price
fun formatPrice(price: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(price)
}