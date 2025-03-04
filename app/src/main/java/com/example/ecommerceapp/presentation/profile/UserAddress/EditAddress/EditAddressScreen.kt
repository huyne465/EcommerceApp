package com.example.ecommerceapp.presentation.profile.UserAddress.EditAddress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAddressScreen(
    navController: NavController,
    addressId: String,
    viewModel: EditAddressViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Load address data when the screen is first displayed
    LaunchedEffect(addressId) {
        viewModel.loadAddress(addressId)
    }

    // Handle success state
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            delay(500) // Show success message briefly
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Address") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.fullName.isEmpty()) {
                // Show loading indicator when initially loading the address
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Main content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Edit Shipping Address",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Form fields
                    OutlinedTextField(
                        value = uiState.fullName,
                        onValueChange = { viewModel.updateFullName(it) },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    OutlinedTextField(
                        value = uiState.phoneNumber,
                        onValueChange = { viewModel.updatePhoneNumber(it) },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    OutlinedTextField(
                        value = uiState.address,
                        onValueChange = { viewModel.updateAddress(it) },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    OutlinedTextField(
                        value = uiState.city,
                        onValueChange = { viewModel.updateCity(it) },
                        label = { Text("City") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.state,
                            onValueChange = { viewModel.updateState(it) },
                            label = { Text("State") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        OutlinedTextField(
                            value = uiState.zipCode,
                            onValueChange = { viewModel.updateZipCode(it) },
                            label = { Text("Zip Code") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                    }

                    // Make default checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Checkbox(
                            checked = uiState.isDefault,
                            onCheckedChange = { viewModel.updateIsDefault(it) }
                        )
                        Text("Use as default shipping address")
                    }

                    // Error message if any
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Save button
                    Button(
                        onClick = {
                            if (viewModel.validateForm()) {
                                viewModel.saveAddress()
                            } else {
                                scope.launch {
                                    // Show error for invalid form
                                    viewModel.uiState.value.copy(
                                        errorMessage = "Please fill all required fields"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(vertical = 8.dp),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Update Address", fontSize = 16.sp)
                        }
                    }

                    // Success message
                    AnimatedVisibility(visible = uiState.isSuccess) {
                        Text(
                            text = "Address updated successfully!",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}