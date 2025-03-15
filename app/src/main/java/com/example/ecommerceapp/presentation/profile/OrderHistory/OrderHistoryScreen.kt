package com.example.ecommerceapp.presentation.profile.OrderHistory

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.ecommerceapp.model.Order
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    navController: NavHostController,
    viewModel: OrderHistoryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSortOrder by viewModel.sortOrder.collectAsState()
    var showDateRangePicker by remember { mutableStateOf(false) }

    // Date range state
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }

    // Filtered orders
    val filteredOrders = remember(uiState.orders, startDate, endDate) {
        if (startDate == null && endDate == null) {
            uiState.orders
        } else {
            uiState.orders.filter { order ->
                val orderDate = Instant.ofEpochMilli(order.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val afterStartDate =
                    startDate?.let { orderDate.isAfter(it) || orderDate.isEqual(it) } ?: true
                val beforeEndDate =
                    endDate?.let { orderDate.isBefore(it) || orderDate.isEqual(it) } ?: true

                afterStartDate && beforeEndDate
            }
        }
    }

    // Apply sort order
    val sortedOrders = remember(filteredOrders, currentSortOrder) {
        viewModel.sortOrders(filteredOrders)
    }

    // Add a LaunchedEffect to reload orders if needed
    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    // Confirm button click for date picker
    val applyDateFilter: () -> Unit = {
        // The filter is applied automatically through State changes
        showDateRangePicker = false
    }

    // Active date range indicator
    val dateRangeText = when {
        startDate != null && endDate != null -> {
            "${startDate!!.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} - ${
                endDate!!.format(
                    DateTimeFormatter.ofPattern("MMM dd, yyyy")
                )
            }"
        }

        startDate != null -> {
            "From ${startDate!!.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
        }

        endDate != null -> {
            "Until ${endDate!!.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
        }

        else -> "All Orders"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Orders") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    // Sort button
                    IconButton(onClick = { viewModel.toggleSortOrder() }) {
                        Icon(
                            imageVector = if (currentSortOrder == OrderHistoryViewModel.SortOrder.NEWEST_FIRST)
                                Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = "Sort by date"
                        )
                    }
                    IconButton(onClick = { showDateRangePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Filter by date"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {


            // Sort order indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (currentSortOrder) {
                        OrderHistoryViewModel.SortOrder.NEWEST_FIRST -> "Newest First"
                        OrderHistoryViewModel.SortOrder.OLDEST_FIRST -> "Oldest First"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Date range filter chip
            if (startDate != null || endDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { showDateRangePicker = true },
                        label = { Text(dateRangeText) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Show date picker"
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Clear filter button
                    IconButton(
                        onClick = {
                            startDate = null
                            endDate = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear date filter",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.errorMessage != null) {
                    ErrorState(message = uiState.errorMessage ?: "")
                } else if (sortedOrders.isEmpty()) {
                    if (uiState.orders.isEmpty()) {
                        EmptyOrdersState(navController)
                    } else {
                        NoFilteredOrdersState(
                            onResetFilters = {
                                startDate = null
                                endDate = null
                            }
                        )
                    }
                } else {
                    OrdersList(sortedOrders, navController)  // Use sortedOrders instead of filteredOrders
                }
            }
        }
    }

    // Date Range Picker Dialog
    if (showDateRangePicker) {
        val datePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli(),
            initialSelectedEndDateMillis = endDate?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli(),
        )

        BasicAlertDialog(
            onDismissRequest = { showDateRangePicker = false },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Filter Orders by Date",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp).align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "Select start and end dates",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).align(Alignment.CenterHorizontally)
                )

                DateRangePicker(
                    state = datePickerState,
                    modifier = Modifier.weight(1f),
                    showModeToggle = false
                )

                // Button row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            datePickerState.selectedStartDateMillis?.let { startMillis ->
                                startDate = Instant.ofEpochMilli(startMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            } ?: run { startDate = null }

                            datePickerState.selectedEndDateMillis?.let { endMillis ->
                                endDate = Instant.ofEpochMilli(endMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            } ?: run { endDate = null }

                            applyDateFilter()
                        }
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
fun NoFilteredOrdersState(onResetFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FilterAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "No Orders Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "No orders were found for the selected date range.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onResetFilters,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text("Reset Filters")
        }
    }
}

@Composable
fun EmptyOrdersState(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "No Orders Yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Your order history will appear here once you make a purchase.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { navController.navigate("home") },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                "Start Shopping",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Oops! Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OrdersList(orders: List<Order>, navController: NavHostController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(orders) { order ->
            OrderCard(
                order,
                onClick = { navController.navigate("order_detail/${order.orderId}") },
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    onMarkAsReceived: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Order header with order ID and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Order #${order.orderId.take(8)}...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        formatTimestampToPattern(order.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = order.status)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )

            // Order summary
            Column(modifier = Modifier.fillMaxWidth()) {
                // Show first item and total count
                val totalItems = order.items.sumOf { it.quantity }
                val firstItem = order.items.firstOrNull()

                if (firstItem != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${firstItem.name} ${if (order.items.size > 1) "and ${order.items.size - 1} more item(s)" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "× $totalItems",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(13.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (firstItem != null) {
                        Text(
                            text = "Size: ${firstItem.selectedSize}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(13.dp))

                // Total price and action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            "$${order.total}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Button row
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mark as Received button (only shown for PENDING orders)
                        if (order.status == "PENDING") {
                            FilledTonalButton(
                                onClick = { onMarkAsReceived(order.orderId) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Text(
                                    "Mark as Received",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // View Details button
                        TextButton(
                            onClick = onClick,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "View Details",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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

// Using the modern date formatting approach
@RequiresApi(Build.VERSION_CODES.O)
fun formatTimestampToPattern(timestamp: Long): String {
    if (timestamp == 0L) return "N/A"

    val instant = Instant.ofEpochMilli(timestamp)
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return DateTimeFormatter.ofPattern("MMMM dd, yyyy | hh:mma").format(dateTime)
}


