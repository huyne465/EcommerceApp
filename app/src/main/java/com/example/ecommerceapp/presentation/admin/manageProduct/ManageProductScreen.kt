package com.example.ecommerceapp.presentation.admin.manageProduct


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.ecommerceapp.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProductScreen(
    navController: NavHostController,
    viewModel: ManageProductViewModel = viewModel(),

    ) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.loadProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Products") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("add_product") }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Product")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.products.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No products available")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { navController.navigate("add_product") }) {
                        Text("Add New Product")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.products) { product ->
                        ManageProductItem(
                            product = product,
                            onEditClick = { navController.navigate("edit_product/${product.id}") },
                            onDeleteClick = { viewModel.deleteProduct(product.id) }
                        )
                    }
                }

                // Show any error messages
                if (uiState.errorMessage != null) {
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Text(uiState.errorMessage!!)
                    }
                }
            }
        }
    }
}

@Composable
fun ManageProductItem(
    product: Product,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
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

                // Show stock information
                Text(
                    text = if (product.stock > 0) "In Stock: ${product.stock}" else "Out of Stock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (product.stock >0 ) MaterialTheme.colorScheme.primary else Color.Red
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
                        text = "${product.price.toInt()}$",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Action buttons
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }

                IconButton(onClick = { onDeleteClick() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}