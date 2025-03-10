package com.example.ecommerceapp.presentation.profile


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64 as AndroidBase64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    navController: NavHostController,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isUploading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri ->
            isUploading = true

            coroutineScope.launch {
                try {
                    // Convert the image to a base64 string
                    val inputStream = context.contentResolver.openInputStream(selectedImageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    // Resize bitmap to reduce storage size
                    val resizedBitmap = resizeBitmap(bitmap, 500) // Max width of 500px

                    // Convert to base64
                    val base64Image = convertBitmapToBase64(resizedBitmap)

                    // Update the profile with the base64 string
                    viewModel.updateProfilePicture(base64Image)

                } catch (e: Exception) {
                    // Handle errors
                    // You might want to add some error messaging here
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My profile") },
                actions = {
                    IconButton(onClick = { /* Open search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        bottomBar = {
            val selectedColor = Color.Gray
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
                        indicatorColor = Color.Black
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
                        indicatorColor = Color.Black
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
                        indicatorColor = Color.Black
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
                        indicatorColor = Color.Black
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = true,
                    onClick = { /* Already on Profile */ },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unSelectedColor,
                        unselectedTextColor = unSelectedColor,
                        indicatorColor = Color.Black
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Profile header with avatar and name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile avatar with click action
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable {
                            // Launch image picker
                            imagePickerLauncher.launch("image/*")
                        }
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else if (uiState.profileImageUrl.isNotEmpty()) {
                        val imageData = remember(uiState.profileImageUrl) {
                            try {
                                // Check if it's a base64 string (doesn't start with http)
                                if (!uiState.profileImageUrl.startsWith("http")) {
                                    // Decode base64 string to bitmap
                                    val decodedBytes = AndroidBase64.decode(
                                        uiState.profileImageUrl,
                                        AndroidBase64.DEFAULT
                                    )
                                    val bitmap = BitmapFactory.decodeByteArray(
                                        decodedBytes,
                                        0,
                                        decodedBytes.size
                                    )
                                    bitmap?.asImageBitmap()
                                } else {
                                    null // Not a base64 string, use Coil for URL
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (imageData != null) {
                            // Use the decoded bitmap directly
                            Image(
                                bitmap = imageData,
                                contentDescription = "Profile picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Use Coil for URL-based images
                            Image(
                                painter = rememberAsyncImagePainter(uiState.profileImageUrl),
                                contentDescription = "Profile picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Camera icon overlay (same as before)
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change profile picture",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(Color(0x80000000), CircleShape)
                                .padding(4.dp),
                            tint = Color.White
                        )
                    } else {
                        // Default placeholder avatar
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.Center),
                            tint = Color.Gray
                        )
                        // Camera icon overlay
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Add profile picture",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(Color(0x80000000), CircleShape)
                                .padding(4.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name and email
                Column {
                    Text(
                        text = uiState.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Profile menu items
            ProfileMenuItem(
                title = "My orders",
                subtitle = "Already have ${uiState.orderCount} orders",
                onClick = { navController.navigate("order_history") }
            )

            ProfileMenuItem(
                title = "Shipping addresses",
                subtitle = "${uiState.addressCount} addresses",
                onClick = {
                    // Navigate to address listing screen
                    navController.navigate("address_list")
                }
            )

            ProfileMenuItem(
                title = "Payment methods",
                subtitle = "Visa *${uiState.lastFourDigits}",
                onClick = { /* Navigate to payment methods */ }
            )

            ProfileMenuItem(
                title = "Promocodes",
                subtitle = "You have special promocodes",
                onClick = { /* Navigate to promocodes */ }
            )

            ProfileMenuItem(
                title = "My reviews",
                subtitle = "Reviews for ${uiState.reviewsCount} items",
                onClick = { /* Navigate to reviews */ }
            )

            ProfileMenuItem(
                title = "Settings",
                subtitle = "Notifications, password",
                onClick = { /* Navigate to settings */ }
            )

            Spacer(modifier = Modifier.height(40.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = {
                        // Sign out
                        viewModel.signOut()
                        navController.navigate("login")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(text = "Sign out", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.Gray
            )
        }

        Divider(
            modifier = Modifier.padding(top = 8.dp),
            thickness = 0.5.dp,
            color = Color.LightGray
        )
    }
}

// Helper function to resize bitmap
fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    if (width <= maxWidth) {
        return bitmap
    }

    val aspectRatio = width.toFloat() / height.toFloat()
    val newHeight = (maxWidth / aspectRatio).toInt()

    return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
}

// Helper function to convert bitmap to base64
fun convertBitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    // Compress to JPEG with 70% quality to reduce size
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
    val byteArray = outputStream.toByteArray()
    return AndroidBase64.encodeToString(byteArray, AndroidBase64.DEFAULT)
}