package com.example.ecommerceapp.presentation.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.ecommerceapp.model.Product


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    viewModel: ShopViewModel = viewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onProductClick: (String) -> Unit,
    navController: NavHostController,
) {

    val context = LocalContext.current

    // Add this LaunchedEffect to force load products when the screen appears
    LaunchedEffect(Unit) {
        viewModel.loadProducts(forceReload = true)
        viewModel.initializeCrisp(context)
    }

    val uiState by viewModel.uiState.collectAsState()


    // Track sorting changes to trigger reload
    LaunchedEffect(uiState.sortOrder) {
        // Only reload if we have products (avoid double-loading on init)
        if (uiState.products.isNotEmpty()) {
            viewModel.loadProducts(forceReload = true)
        }
    }


    // Track filter category changes
    LaunchedEffect(uiState.filterCategory) {
        if (uiState.products.isNotEmpty()) {
            viewModel.loadProducts(forceReload = true)
        }
    }

    // Track search query changes
    LaunchedEffect(uiState.searchQuery) {
        if (uiState.searchQuery.length >= 2 && uiState.products.isNotEmpty()) {
            viewModel.loadProducts(forceReload = true)
        }
    }

    val filteredProducts = viewModel.getFilteredAndSortedProducts()
    val categories = listOf("T-shirts", "Crop tops", "Sleeveless", "More")

    // Update the selectedCategory based on persistentFilterCategory
    var selectedCategory by remember { mutableStateOf(uiState.filterCategory) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(uiState.searchQuery) }
    var showSortOptions by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            if (showSearchBar) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        viewModel.setSearchQuery(it)
                    },
                    onSearchClose = {
                        showSearchBar = false
                        // Clear the search query when closing
                        if (searchQuery.isNotEmpty()) {
                            searchQuery = ""
                            viewModel.setSearchQuery("")
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("Shop") },
                    actions = {
                        // Add chat button
                        IconButton(onClick = {
//                            viewModel.resetCrispChat(context)
                            viewModel.openCrispChat(context)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Customer Support")
                        }
                        // Add refresh button here
                        IconButton(onClick = { viewModel.loadProducts(forceReload = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFF0F0F0)) {
                val selectedColor = Color.Gray // Màu bạc
                val unSelectedColor = Color.DarkGray
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
                        indicatorColor = Color.Black // Để không có hiệu ứng nền khi chọn
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Shop") },
                    label = { Text("Shop") },
                    selected = true,
                    onClick = { /* Already on Shop */ },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unSelectedColor,
                        unselectedTextColor = unSelectedColor,
                        indicatorColor = Color.Black // Để không có hiệu ứng nền khi chọn
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Bag") },
                    label = { Text("Bag") },
                    selected = false,
                    onClick = onNavigateToCart,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unSelectedColor,
                        unselectedTextColor = unSelectedColor,
                        indicatorColor = Color.Black // Để không có hiệu ứng nền khi chọn
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
                        indicatorColor = Color.Black // Để không có hiệu ứng nền khi chọn
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
                        indicatorColor = Color.Black // Để không có hiệu ứng nền khi chọn
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Categories
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory

                    Surface(
                        modifier = Modifier.clickable {
                            selectedCategory = if (isSelected) null else category
                            viewModel.setFilterCategory(if (isSelected) null else category)
                        },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White
                        )
                    }
                }
            }

            // Filters and Sort
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* Open filters */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Filters",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filters")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        showSortOptions = !showSortOptions
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(getSortOrderText(uiState.sortOrder))
                }

                IconButton(onClick = { /* Switch view */ }) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Grid View"
                    )
                }
            }

            // Sort options dropdown menu
            if (showSortOptions) {
                SortOptionsMenu(
                    onSortOrderSelected = { sortOrder ->
                        viewModel.setSortOrder(sortOrder)
                        showSortOptions = false
                    },
                    onResetSort = {
                        viewModel.resetSort()
                        showSortOptions = false
                    },
                    onDismiss = { showSortOptions = false }
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No products found")
                        if (uiState.searchQuery.isNotEmpty() || uiState.filterCategory != null || uiState.sortOrder != ShopViewModel.SortOrder.NONE) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.setSearchQuery("")
                                    viewModel.setFilterCategory(null)
                                    viewModel.resetSort()
                                    selectedCategory = null
                                    searchQuery = ""
                                }
                            ) {
                                Text("Clear All Filters")
                            }
                        }
                    }
                }
            } else {
                // Product list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductItem(
                            product = product,
                            onProductClick = { onProductClick(product.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(product.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search products by name...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onSearchClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        }
    )
}

@Composable
fun SortOptionsMenu(
    onSortOrderSelected: (ShopViewModel.SortOrder) -> Unit,
    onResetSort: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.titleMedium
                )

                // Reset button
                TextButton(
                    onClick = onResetSort,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider()

            SortOption(text = "Name: A to Z") {
                onSortOrderSelected(ShopViewModel.SortOrder.NAME_A_TO_Z)
            }

            SortOption(text = "Name: Z to A") {
                onSortOrderSelected(ShopViewModel.SortOrder.NAME_Z_TO_A)
            }

            SortOption(text = "Price: Low to High") {
                onSortOrderSelected(ShopViewModel.SortOrder.PRICE_LOW_TO_HIGH)
            }

            SortOption(text = "Price: High to Low") {
                onSortOrderSelected(ShopViewModel.SortOrder.PRICE_HIGH_TO_LOW)
            }

            SortOption(text = "Rating") {
                onSortOrderSelected(ShopViewModel.SortOrder.RATING)
            }
        }
    }
}

@Composable
fun SortOption(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text)
    }
}

@Composable
fun ProductItem(
    product: Product,
    onProductClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    key(product.id, product.isFavorite) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onProductClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                // Product Image
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
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
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    // Star Rating
                    Row {
                        repeat(5) { index ->
                            val isFilled = index < product.rating
                            Icon(
                                imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (isFilled) Color(0xFFFFD700) else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${product.reviewCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Text(
                        text = "${product.price.toInt()}$",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Favorite Button
                IconButton(
                    onClick = { onFavoriteClick() },
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (product.isFavorite) Color.Red else Color.Gray
                    )
                }
            }
        }
    }
}

// Helper function to get readable text for sort order
fun getSortOrderText(sortOrder: ShopViewModel.SortOrder): String {
    return when (sortOrder) {
        ShopViewModel.SortOrder.NAME_A_TO_Z -> "Name: A to Z"
        ShopViewModel.SortOrder.NAME_Z_TO_A -> "Name: Z to A"
        ShopViewModel.SortOrder.PRICE_LOW_TO_HIGH -> "Price: Low to High"
        ShopViewModel.SortOrder.PRICE_HIGH_TO_LOW -> "Price: High to Low"
        ShopViewModel.SortOrder.RATING -> "Rating"
        ShopViewModel.SortOrder.NONE -> "Sort"
    }
}