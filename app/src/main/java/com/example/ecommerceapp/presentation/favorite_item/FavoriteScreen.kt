package com.example.ecommerceapp.presentation.favorite_item

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ecommerceapp.model.Product
import com.example.ecommerceapp.presentation.shop.ProductItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    viewModel: FavoriteViewModel = viewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Favorites") }
            )
        },
        bottomBar = {
            val selectedColor = Color.Gray // Màu bạc
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
                        indicatorColor = Color.Black // Để không có hiệu ứng nền khi chọn
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
                    selected = true,
                    onClick = { /* Already on Favorites */ },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "Unknown error occurred",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                uiState.favoriteProducts.isEmpty() -> {
                    EmptyFavoritesMessage(
                        onNavigateToShop = onNavigateToShop,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    FavoriteProductsList(
                        products = uiState.favoriteProducts,
                        onProductClick = onProductClick,
                        onFavoriteClick = { productId -> viewModel.removeFavorite(productId) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyFavoritesMessage(
    onNavigateToShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No favorites yet",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Items you like will be saved here",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateToShop) {
            Text("Continue Shopping")
        }
    }
}

@Composable
fun FavoriteProductsList(
    products: List<Product>,
    onProductClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products) { product ->
            ProductItem(
                product = product,
                onProductClick = { onProductClick(product.id) },
                onFavoriteClick = { onFavoriteClick(product.id) }
            )
        }
    }
}