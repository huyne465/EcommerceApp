package com.example.ecommerceapp.presentation.shop.productDetail

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ecommerceapp.model.MediaType
import com.example.ecommerceapp.model.ProductComment


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    viewModel: ProductDetailViewModel = viewModel(
        factory = ProductDetailViewModelFactory(
            LocalContext.current.applicationContext as Application,
            productId
        )
    ),
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showRatingDialog by remember { mutableStateOf(false) }
    val hasUserReviewed by viewModel.hasUserReviewed.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (uiState.product?.isFavorite == true)
                                Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (uiState.product?.isFavorite == true) Color.Red else Color.Gray
                        )
                    }
                    IconButton(onClick = onNavigateToCart) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadProduct() }) {
                        Text("Retry")
                    }
                }
            } else {
                uiState.product?.let { product ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        // Product Image
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Brand
                        Text(
                            text = product.brand,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Product Name
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rating Section
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = { showRatingDialog = true })
                        ) {
                            // Display stars based on rating
                            repeat(5) { index ->
                                val isFilled = index < product.rating
                                Icon(
                                    imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    contentDescription = null,
                                    tint = if (isFilled) Color(0xFFFFD700) else Color.LightGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${product.reviewCount} reviews)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Price
                        Text(
                            text = "$${product.price.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quantity Selector
                        QuantitySelector(
                            quantity = uiState.quantity,
                            onIncrement = { viewModel.incrementQuantity() },
                            onDecrement = { viewModel.decrementQuantity() }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Description
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Add to Cart Button
                        Button(
                            onClick = { viewModel.addToCart() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add to Cart",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))


                        // Comments Section
                        Text(
                            text = "Reviews",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Display comments or prompt to add first comment
                        if (uiState.comments.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No reviews yet",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(onClick = { showRatingDialog = true }) {
                                        Text("Be the first to review")
                                    }
                                }
                            }
                        } else {
                            // Display existing comments
                            CommentsList(comments = uiState.comments)

                            // Add a button to add a new comment
                            OutlinedButton(
                                onClick = { showRatingDialog = true },
                                enabled = !hasUserReviewed,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (hasUserReviewed) "You've already reviewed this product" else "Add Review"
                                )
                            }
                        }

                        // Display rating dialog when needed
                        if (showRatingDialog) {
                            RatingDialog(
                                currentRating = uiState.product?.rating ?: 0,
                                onDismiss = { showRatingDialog = false },
                                onRatingSubmit = { rating, comment, mediaUris ->
                                    viewModel.submitRatingWithComment(rating, comment, mediaUris)
                                    showRatingDialog = false
                                }
                            )
                        }
                    }
                }


                // Show snackbar for actions
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
}

@Composable
fun QuantitySelector(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Quantity:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = quantity > 1
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease quantity"
                )
            }

            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onIncrement) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase quantity"
                )
            }
        }
    }
}

@Composable
fun RatingDialog(
    currentRating: Int,
    onDismiss: () -> Unit,
    onRatingSubmit: (Int, String, List<Uri>) -> Unit,
) {
    var selectedRating by remember { mutableIntStateOf(currentRating) }
    var comment by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val scrollState = rememberScrollState()
    var showRatingDialog by remember { mutableStateOf(false) }

    // Image/video picker launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        // Limit to 3 media items
        selectedMediaUris = uris.take(3)
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate this product") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Tap to select your rating")
                Spacer(modifier = Modifier.height(16.dp))

                // Rating stars
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(5) { index ->
                        IconButton(onClick = { selectedRating = index + 1 }) {
                            Icon(
                                imageVector = if (index < selectedRating)
                                    Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Star ${index + 1}",
                                tint = if (index < selectedRating) Color(0xFFFFD700) else Color.LightGray,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Comment text field
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Your review") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Media upload section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Add photos/videos",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedButton(
                        onClick = { mediaPickerLauncher.launch("image/* video/*") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Add media"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload")
                    }
                }

                // Preview selected media
                if (selectedMediaUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        selectedMediaUris.forEach { uri ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected media",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Delete button overlay
                                IconButton(
                                    onClick = {
                                        selectedMediaUris = selectedMediaUris.filterNot { it == uri }
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color(0x80000000), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedMediaUris.isNotEmpty()) {
                    Text(
                        "${selectedMediaUris.size}/3 files selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRatingSubmit(selectedRating, comment, selectedMediaUris) }
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CommentsList(comments: List<ProductComment>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        comments.forEach { comment ->
            CommentItem(comment = comment)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
fun CommentItem(comment: ProductComment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // User profile section with image, name and rating
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User profile image
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (comment.userProfileUrl.isNotBlank()) {
                    val imageData = remember(comment.userProfileUrl) {
                        try {
                            // Check if it's a base64 string (doesn't start with http)
                            if (!comment.userProfileUrl.startsWith("http")) {
                                // Decode base64 string to bitmap
                                val decodedBytes = Base64.decode(
                                    comment.userProfileUrl,
                                    Base64.DEFAULT
                                )
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(
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
                        // Use the decoded bitmap directly
                        androidx.compose.foundation.Image(
                            bitmap = imageData,
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Use AsyncImage for URL-based images
                        AsyncImage(
                            model = comment.userProfileUrl,
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // Default placeholder avatar
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile image",
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Username and rating
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Comment date
                Text(
                    text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(comment.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Display user's rating
            Row {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < comment.rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = if (index < comment.rating) Color(0xFFFFD700) else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Comment content
        if (comment.comment.isNotBlank()) {
            Text(
                text = comment.comment,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 48.dp) // Align with the profile image
            )
        }

        // Display media content if available
        if (comment.mediaUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                comment.mediaUrls.forEach { media ->
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                // Handle media viewing (expanded image/video player)
                            }
                    ) {
                        // Check if it's a base64 string
                        val mediaData = remember(media.url) {
                            try {
                                if (!media.url.startsWith("http")) {
                                    // Decode base64 string
                                    val decodedBytes = Base64.decode(
                                        media.url,
                                        Base64.DEFAULT
                                    )
                                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                                        decodedBytes,
                                        0,
                                        decodedBytes.size
                                    )
                                    bitmap?.asImageBitmap()
                                } else {
                                    null // Will use AsyncImage for URL
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (mediaData != null) {
                            // For base64 encoded images
                            androidx.compose.foundation.Image(
                                bitmap = mediaData,
                                contentDescription = "Review image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (media.type == MediaType.IMAGE) {
                            // For URL-based images
                            AsyncImage(
                                model = media.url,
                                contentDescription = "Review image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // For videos
                            val thumbnailData = remember(media.thumbnailUrl) {
                                try {
                                    if (!media.thumbnailUrl.startsWith("http")) {
                                        val decodedBytes = Base64.decode(
                                            media.thumbnailUrl,
                                            Base64.DEFAULT
                                        )
                                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                                            decodedBytes,
                                            0,
                                            decodedBytes.size
                                        )
                                        bitmap?.asImageBitmap()
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (thumbnailData != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = thumbnailData,
                                    contentDescription = "Review video thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                AsyncImage(
                                    model = media.thumbnailUrl,
                                    contentDescription = "Review video thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Play button overlay
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = "Play video",
                                modifier = Modifier
                                    .size(40.dp)
                                    .align(Alignment.Center),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
