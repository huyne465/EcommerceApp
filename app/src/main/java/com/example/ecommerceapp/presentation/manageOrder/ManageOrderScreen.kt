package com.example.ecommerceapp.presentation.manageOrder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.ecommerceapp.model.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageOrderScreen(
    navController: NavHostController,
    viewModel: ManageOrderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Orders") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPendingOrders() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.pendingOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No pending orders to manage",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Pending Orders (${uiState.pendingOrders.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.pendingOrders) { order ->
                        PendingOrderItem(
                            order = order,
                            onConfirmClick = { viewModel.confirmOrder(order) }
                        )
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearErrorMessage() }) {
                            Text("Dismiss")
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(uiState.errorMessage ?: "")
                }
            }
        }
    }
}

@Composable
fun PendingOrderItem(order: Order, onConfirmClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.orderId.take(8)}...",
                    style = MaterialTheme.typography.titleMedium
                )

                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Customer: ${order.shippingAddress.fullName}",
                style = MaterialTheme.typography.bodyMedium
            )

            val date = Date(order.timestamp)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            Text(
                text = "Date: ${dateFormat.format(date)}",
                style = MaterialTheme.typography.bodyMedium
            )

            // Display shipping address
            val addressParts = listOf(
                order.shippingAddress.address,
                order.shippingAddress.city,
                order.shippingAddress.state,
                order.shippingAddress.zipCode
            ).filter { it.isNotEmpty() }

            if (addressParts.isNotEmpty()) {
                Text(
                    text = "Ship to: ${addressParts.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Display items summary
            val itemCount = order.items.size
            Text(
                text = "Items: $itemCount (${order.items.sumOf { it.quantity }} total units)",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: $${order.total}",
                    style = MaterialTheme.typography.titleMedium
                )

                Button(
                    onClick = onConfirmClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Confirm Order")
                }
            }
        }
    }
}


@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "PAID" -> Color(0xFF4CAF50) to Color.White
        "PENDING" -> Color(0xFFFF9800) to Color.Black
        "CANCELED" -> Color(0xFFF44336) to Color.White
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}