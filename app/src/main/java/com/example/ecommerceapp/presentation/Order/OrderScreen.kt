package com.example.ecommerceapp.presentation.Order

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.example.ecommerceapp.presentation.Cart.formatPrice
import com.example.ecommerceapp.presentation.Order.OrderViewModel.PaymentMethod
import androidx.compose.foundation.layout.size
import com.example.ecommerceapp.presentation.profile.UserAddress.AddressList.AddressListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    viewModel: OrderViewModel = viewModel(),
    addressListViewModel: AddressListViewModel = viewModel(),
    navController: NavHostController,
    onOrderComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showOrderConfirmation by remember { mutableStateOf(false) }
    val addressUiState by addressListViewModel.uiState.collectAsState()

    // Force immediate loading when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadCartItems(forceReload = true)
    }

    LaunchedEffect(Unit) {
        addressListViewModel.loadAddresses()
    }

    LaunchedEffect(addressUiState.addresses) {
    }


    LaunchedEffect(uiState.cartItems) {
        viewModel.recalculateOrderTotals()
    }

    // Handle order completed
    LaunchedEffect(uiState.orderPlaced) {
        if (uiState.orderPlaced) {
            onOrderComplete()
        }
    }

    // Show confirmation dialog before placing order
    if (showOrderConfirmation) {
        AlertDialog(
            onDismissRequest = { showOrderConfirmation = false },
            title = { Text("Confirm Order") },
            text = {
                Column {
                    Text("Total: ${formatPrice(uiState.total)}")
                    Text("Payment Method: ${uiState.selectedPaymentMethod.displayName}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Are you sure you want to place this order?")
                }
            },
            confirmButton = {
                Button(onClick = {
                    val defaultAddress = addressUiState.addresses.find { it.isDefault }
                    if (defaultAddress != null) {
                        viewModel.placeOrder(defaultAddress.id)
                        showOrderConfirmation = false
                    } else {
                        // Handle case where no default address is found
                        // For example, show an error message
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOrderConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Return to Cart")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Shipping Address Section
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable(
                                    onClick = {
                                        // Navigate to address list instead of select_address
                                        navController.navigate("address_list")
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Shipping Address",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Change",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                // Display default address if available
                                addressUiState.addresses.find { it.isDefault }
                                    ?.let { defaultAddress ->
                                        Row {
                                            Text(
                                                text = "${defaultAddress.fullName}, ${defaultAddress.address}, ${defaultAddress.city}, ${defaultAddress.state}, ${defaultAddress.zipCode}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                        }
                                    }
                            }
                        }
                    }

                    // Order Items Section
                    item {
                        Text(
                            text = "Order Summary",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Cart Items
                    if (uiState.cartItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Your cart is empty",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        items(uiState.cartItems) { cartItem ->
                            OrderItemCard(cartItem = cartItem)
                        }
                    }

                    // Payment Method Selection
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Payment Method",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                PaymentMethodItem(
                                    title = "Cash On Delivery",
                                    description = "Pay when you receive",
                                    isSelected = uiState.selectedPaymentMethod == PaymentMethod.COD,
                                    onClick = { viewModel.selectPaymentMethod(PaymentMethod.COD) }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PaymentMethodItem(
                                    title = "ZaloPay",
                                    description = "Pay with ZaloPay",
                                    isSelected = uiState.selectedPaymentMethod == PaymentMethod.ZALO_PAY,
                                    onClick = { viewModel.selectPaymentMethod(PaymentMethod.ZALO_PAY) }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                PaymentMethodItem(
                                    title = "Google Pay",
                                    description = "Fast and secure checkout",
                                    isSelected = uiState.selectedPaymentMethod == PaymentMethod.GOOGLE_PAY,
                                    onClick = { viewModel.selectPaymentMethod(PaymentMethod.GOOGLE_PAY) }
                                )
                            }
                        }
                    }

                    // Order Total
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Subtotal",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = formatPrice(uiState.subtotal),
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
                                        text = formatPrice(uiState.tax),
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
                                        text = formatPrice(uiState.shipping),
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
                                        text = formatPrice(uiState.total),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Place Order Button
                    item {
                        Button(
                            onClick = { showOrderConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(vertical = 8.dp),
                            enabled =
                            uiState.cartItems.isNotEmpty() &&
                                    !uiState.isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isProcessing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Place Order",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // Show any action messages as a Snackbar
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
        }
    }
}

@Composable
fun OrderItemCard(cartItem: CartItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            AsyncImage(
                model = cartItem.imageUrl,
                contentDescription = cartItem.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Product Details
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = cartItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Qty: ${cartItem.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Price
            Text(
                text = formatPrice(cartItem.price * cartItem.quantity),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PaymentMethodItem(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

