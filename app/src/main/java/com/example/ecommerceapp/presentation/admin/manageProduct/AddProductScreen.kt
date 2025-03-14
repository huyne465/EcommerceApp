package com.example.ecommerceapp.presentation.admin.manageProduct

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.style.TextAlign

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AddProductScreen(
    viewModel: AddProductViewModel = viewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("T-shirts", "Crop tops", "Sleeveless", "Pullover", "Blouse", "Shirt")

    // Effect để xử lý khi thêm sản phẩm thành công
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            // Hiển thị thông báo thành công
            delay(500)  // Đợi 0.5 giây
            onNavigateBack()  // Quay lại màn hình trước
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm sản phẩm mới") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form fields
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Tên sản phẩm") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.name.isEmpty() && uiState.errorMessage != null
            )

            OutlinedTextField(
                value = uiState.brand,
                onValueChange = viewModel::onBrandChange,
                label = { Text("Thương hiệu") },
                modifier = Modifier.fillMaxWidth()
            )

            // Category dropdown
            var expanded by remember { mutableStateOf(false) }


            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    value = viewModel.uiState.value.selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                )

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
                label = { Text("Mô tả") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = uiState.price,
                onValueChange = viewModel::onPriceChange,
                label = { Text("Giá ($)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            OutlinedTextField(
                value = uiState.imageUrl,
                onValueChange = viewModel::onImageUrlChange,
                label = { Text("URL hình ảnh") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.stock,
                onValueChange = viewModel::onStockChange,
                label = { Text("Stock quantity") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Submit button
            Button(
                onClick = { viewModel.addProduct() },
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
                    Text("Thêm sản phẩm")
                }
            }


        }
    }

}
