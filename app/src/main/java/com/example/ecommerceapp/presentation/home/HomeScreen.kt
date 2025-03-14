import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.ecommerceapp.R
import com.example.ecommerceapp.presentation.home.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier, navController: NavHostController,
    userId: String, // Pass the user ID as a parameter
    viewModel: HomeViewModel = viewModel() // Use HomeViewModel
) {
    val adminId = "zokfRMBO0eZsE6yRWi3TsDOj2KS2"
    val coroutineScope = rememberCoroutineScope()

    if (userId == adminId) {
        ModalNavigationDrawer(
            drawerState = viewModel.drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    // Drawer content for admin
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Admin Panel", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Manage Products", modifier = Modifier.clickable {
                            navController.navigate("manage_products")
                            coroutineScope.launch { viewModel.drawerState.close() }
                        })
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Orders Manage", modifier = Modifier.clickable {
                            coroutineScope.launch { viewModel.drawerState.close() }
                        })
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("User Manage", modifier = Modifier.clickable {
                            coroutineScope.launch { viewModel.closeDrawer(coroutineScope) }
                        })
                        // Add more admin options here
                    }
                }
            }
        ) {
            // Main content
            HomeContent(modifier, navController, viewModel)
        }
    } else {
        // Main content without drawer
        HomeContent(modifier, navController, viewModel)
    }
}

//Home Content
@Composable
fun HomeContent(modifier: Modifier, navController: NavHostController, viewModel: HomeViewModel) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item { HeroBanner() }
                item { SectionHeader(title = "New", subtitle = "You've never seen it before!", showViewAll = true) }
                item { NewArrivalsSection() }
                item { SectionHeader(title = "Trending", subtitle = "Popular this week", showViewAll = true) }
                item { TrendingSection() }
                item { SectionHeader(title = "Categories", subtitle = "Find your style", showViewAll = false) }
                item { CategoriesSection() }
                item { SectionHeader(title = "Popular Brands", subtitle = "Top fashion houses", showViewAll = true) }
                item { BrandsSection() }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun HeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Banner image
        Image(
            painter = painterResource(id = R.drawable.banner), // Placeholder
            contentDescription = "Fashion Sale Banner",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay to make text readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // Text and button
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = "Fashion sale",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Check", color = Color.White)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String, showViewAll: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        if (showViewAll) {
            Text(
                text = "View all",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun NewArrivalsSection() {
    val newItems = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(newItems) { item ->
            NewArrivalItem()
        }
    }
}

@Composable
fun NewArrivalItem() {
    Column(
        modifier = Modifier.width(120.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray)
        ) {
            // New badge
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "NEW",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Fashion Item",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "$49.99",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red
        )
    }
}

@Composable
fun TrendingSection() {
    val trendingItems = listOf("Trend 1", "Trend 2", "Trend 3", "Trend 4")

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(trendingItems) { item ->
            TrendingItem()
        }
    }
}

@Composable
fun TrendingItem() {
    Column(
        modifier = Modifier.width(160.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Trending Fashion",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$79.99",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$99.99",
                fontSize = 12.sp,
                color = Color.Gray,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
            )
        }
    }
}

@Composable
fun CategoriesSection() {
    val categories = listOf("Women", "Men", "Kids", "Accessories", "Shoes", "Sports")

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        categories.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCategories.forEach { category ->
                    CategoryItem(
                        name = category,
                        modifier = Modifier.weight(1f)
                    )
                }

                // If odd number of categories, add empty space
                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CategoryItem(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun BrandsSection() {
    val brands = listOf("Brand 1", "Brand 2", "Brand 3", "Brand 4", "Brand 5")

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(brands) { brand ->
            BrandItem(name = brand)
        }
    }
}

@Composable
fun BrandItem(name: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = name,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val selectedColor = Color.Gray // Màu bạc
    val unSelectedColor = Color.DarkGray
    NavigationBar(
        containerColor = Color(0xFFF0F0F0)
    ) {
        NavigationBarItem(
            selected = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                selectedTextColor = selectedColor,
                unselectedIconColor = unSelectedColor,
                unselectedTextColor = unSelectedColor,
                indicatorColor = Color.Black // Để không có hiệu ứng nền khi chọn
            ),
            onClick = { },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                selectedTextColor = selectedColor,
                unselectedIconColor = unSelectedColor,
                unselectedTextColor = unSelectedColor,
                indicatorColor = Color.Black // Để không có hiệu ứng nền khi chọn
            ),
            onClick = {
                // Add navigation logic here
                navController.navigate("shop") {
                    // Optional: Configure navigation options
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Shop") },
            label = { Text("Shop") }
        )
        NavigationBarItem(
            selected = false,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                selectedTextColor = selectedColor,
                unselectedIconColor = unSelectedColor,
                unselectedTextColor = unSelectedColor,
                indicatorColor = Color.Black // Để không có hiệu ứng nền khi chọn
            ),
            onClick = {
                // Add navigation logic here
                navController.navigate("cart") {
                    // Optional: Configure navigation options
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Bag") },
            label = { Text("Bag") }
        )
        NavigationBarItem(
            selected = false,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                selectedTextColor = selectedColor,
                unselectedIconColor = unSelectedColor,
                unselectedTextColor = unSelectedColor,
                indicatorColor = Color.Black // Để không có hiệu ứng nền khi chọn
            ),
            onClick = {
                // Add navigation logic here
                navController.navigate("favorites") {
                    // Optional: Configure navigation options
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
            label = { Text("Favorites") }
        )
        NavigationBarItem(
            selected = false,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                selectedTextColor = selectedColor,
                unselectedIconColor = unSelectedColor,
                unselectedTextColor = unSelectedColor,
                indicatorColor = Color.Black // Để không có hiệu ứng nền khi chọn
            ),
            onClick = {
                navController.navigate("profile") {
                    // Optional: Configure navigation options
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}


