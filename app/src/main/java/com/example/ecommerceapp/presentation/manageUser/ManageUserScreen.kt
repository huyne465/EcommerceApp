package com.example.ecommerceapp.presentation.manageUser

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import android.util.Base64 as AndroidBase64
import com.example.ecommerceapp.model.UserModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUserScreen(
    navController: NavHostController,
    viewModel: ManageUserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load users when the screen first appears
    LaunchedEffect(key1 = true) {
        viewModel.loadUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Users") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            } else if (uiState.users.isEmpty()) {
                Text(
                    "No users found",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.users) { user ->
                        UserItem(
                            user = user,
                            onToggleBan = { viewModel.toggleUserBanStatus(user.id, user.banned) }
                        )
                    }
                }
            }

            // Show error message if any
            if (uiState.errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Text(text = uiState.errorMessage ?: "")
                }
                LaunchedEffect(uiState.errorMessage) {
                    viewModel.clearErrorMessage()
                }
            }

            // Show action message if any
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
fun UserItem(
    user: UserModel,
    onToggleBan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Avatar - handling both URL and base64
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                if (user.photoUrl.isNotEmpty()) {
                    val imageData = remember(user.photoUrl) {
                        try {
                            // Check if it's a base64 string (doesn't start with http)
                            if (!user.photoUrl.startsWith("http")) {
                                // Decode base64 string to bitmap
                                val decodedBytes = AndroidBase64.decode(
                                    user.photoUrl,
                                    AndroidBase64.DEFAULT
                                )
                                val bitmap = BitmapFactory.decodeByteArray(
                                    decodedBytes,
                                    0,
                                    decodedBytes.size
                                )
                                bitmap?.asImageBitmap()
                            } else {
                                null // Not a base64 string, use AsyncImage for URL
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (imageData != null) {
                        // Use Coil for URL-based images
                        Image(
                            painter = rememberAsyncImagePainter(user.photoUrl),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Use AsyncImage for URL-based images
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // Default avatar placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                // Ban status indicator
                if (user.banned) {
                    Text(
                        text = "Banned",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Ban/Unban Button
            IconButton(
                onClick = onToggleBan,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (user.banned) MaterialTheme.colorScheme.primary else Color.Red
                )
            ) {
                Icon(
                    imageVector = if (user.banned) Icons.Default.Check else Icons.Default.Block,
                    contentDescription = if (user.banned) "Unban User" else "Ban User"
                )
            }
        }
    }
}