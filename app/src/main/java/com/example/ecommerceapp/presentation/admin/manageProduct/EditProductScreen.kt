package com.example.ecommerceapp.presentation.admin.manageProduct

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun EditProductScreen(
    navController: NavHostController,
    productId: String,
    viewModel: EditProductViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("T-shirts", "Crop tops", "Sleeveless", "Pullover", "Blouse", "Shirt")

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Product") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.name.isEmpty() && uiState.errorMessage != null
                )

                OutlinedTextField(
                    value = uiState.brand,
                    onValueChange = viewModel::onBrandChange,
                    label = { Text("Brand") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown
                var expanded by remember { mutableStateOf(false) }


                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    uiState.category?.let {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            value = it,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        )
                    }

                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
                        categories.forEachIndexed { index, text ->
                            DropdownMenuItem(
                                onClick = {
                                    viewModel.onSelectedCategoryChange(categories[index])
                                    expanded = false
                                },
                                text = {
                                    Text(text)
                                },

                                )

                        }

                    }
                }

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = uiState.price,
                    onValueChange = viewModel::onPriceChange,
                    label = { Text("Price ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = uiState.imageUrl,
                    onValueChange = viewModel::onImageUrlChange,
                    label = { Text("Image URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.stock,
                    onValueChange = viewModel::onStockChange,
                    label = { Text("Stock quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = { viewModel.updateProduct() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading &&
                            uiState.name.isNotBlank() &&
                            uiState.price.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Update Product")
                    }
                }
            }
        }
    }
}