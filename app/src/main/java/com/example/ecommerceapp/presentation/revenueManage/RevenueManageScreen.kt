package com.example.ecommerceapp.presentation.revenueManage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.ecommerceapp.model.Order
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueManageScreen(
    navController: NavHostController,
    viewModel: RevenueManageViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revenue Management") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                DateRangePicker(
                    startDate = uiState.startDate,
                    endDate = uiState.endDate,
                    onDateRangeSelected = { startDate, endDate ->
                        viewModel.setDateRange(startDate, endDate)
                    },
                    onClearDateFilter = {
                        viewModel.clearDateFilter()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Total Revenue: \$${uiState.totalRevenue}",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(uiState.orders) { order ->
                        OrderItem(order = order)
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
fun DateRangePicker(
    startDate: Date?,
    endDate: Date?,
    onDateRangeSelected: (Date, Date) -> Unit,
    onClearDateFilter: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var startDateText by remember { mutableStateOf(startDate?.let { dateFormat.format(it) } ?: "") }
    var endDateText by remember { mutableStateOf(endDate?.let { dateFormat.format(it) } ?: "") }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Filter by Date Range",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = startDateText,
                onValueChange = { startDateText = it },
                label = { Text("Start Date (yyyy-MM-dd)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = endDateText,
                onValueChange = { endDateText = it },
                label = { Text("End Date (yyyy-MM-dd)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    try {
                        val parsedStartDate = dateFormat.parse(startDateText)
                        val parsedEndDate = dateFormat.parse(endDateText)
                        if (parsedStartDate != null && parsedEndDate != null) {
                            onDateRangeSelected(parsedStartDate, parsedEndDate)
                        }
                    } catch (e: Exception) {
                        // Handle invalid date format error
                    }
                },
                enabled = startDateText.isNotEmpty() && endDateText.isNotEmpty()
            ) {
                Text("Apply Filter")
            }

            TextButton(onClick = onClearDateFilter) {
                Text("Clear Filter")
            }
        }
    }
}

@Composable
fun OrderItem(order: Order) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Order ID: ${order.orderId}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Customer: ${order.shippingAddress.fullName}",
                style = MaterialTheme.typography.titleMedium
            )

            val date = Date(order.timestamp)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            Text(text = "Date: ${dateFormat.format(date)}")

            // Format and display shipping address
            val addressParts = listOf(
                order.shippingAddress.address,
                order.shippingAddress.city,
                order.shippingAddress.state,
                order.shippingAddress.zipCode
            ).filter { it.isNotEmpty() }

            if (addressParts.isNotEmpty()) {
                Text(text = "Ship to: ${addressParts.joinToString(", ")}")
            }
            Spacer(modifier = Modifier.height(4.dp))

            Row( modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically ) {
                Text(
                    text = "Total Amount: \$${order.total}",
                    style = MaterialTheme.typography.titleLarge
                )
                StatusBadge(order.status)
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
