package com.example.ecommerceapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ecommerceapp.presentation.Cart.CartScreen
import com.example.ecommerceapp.presentation.Shop.ProductDetail.ProductDetailScreen
import com.example.ecommerceapp.presentation.Shop.ProductManage.AddProductScreen
import com.example.ecommerceapp.presentation.Shop.ShopScreen
import com.example.ecommerceapp.presentation.AuthScreen
import com.example.ecommerceapp.presentation.favorite_item.FavoriteScreen
import com.example.ecommerceapp.presentation.profile.ProfileScreen
import com.example.ecommerceapp.presentation.profile.UserAddress.AddAddress.AddAddressScreen
import com.example.ecommerceapp.presentation.profile.UserAddress.AddressList.AddressListScreen
import com.example.ecommerceapp.presentation.profile.UserAddress.EditAddress.EditAddressScreen
import com.example.ecommerceapp.presentation.sign_in.SignInScreen
import com.example.ecommerceapp.presentation.sign_up.SignUpScreen
import com.example.ecommerceapp.presentation.sign_in.SignInViewModel
import com.example.ecommerceapp.presentation.sign_up.SignUpViewModel
import homeScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val signInViewModel = viewModel(modelClass = SignInViewModel::class.java)
    val signUpViewModel = viewModel(modelClass = SignUpViewModel::class.java)

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(modifier, navController)
        }

        composable("login") {
            SignInScreen(modifier, signInViewModel, navController)
        }

        composable("signUp") {
            SignUpScreen(modifier, signUpViewModel, navController)
        }

        composable("home") {
            homeScreen(modifier, navController)
        }

        composable("shop") {
            ShopScreen(
                navController = navController,
                onNavigateToHome = { navController.navigate("home") },
                onAddProductClick = { navController.navigate("add_product") },
                onNavigateToCart = { navController.navigate("cart") },
                onNavigateToFavorites = { navController.navigate("favorites") },
                onNavigateToProfile = { navController.navigate("profile") },
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                }
            )
        }

        composable("add_product") {
            AddProductScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "product_detail/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate("cart") }
            )
        }

        composable("cart") {
            CartScreen(
                navController = navController,
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToShop = { navController.navigate("shop") },
                onNavigateToFavorites = { navController.navigate("favorites") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToProduct = { productId ->
                    navController.navigate("product_detail/$productId")
                }
            )
        }

        composable("favorites") {
            FavoriteScreen(
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToShop = { navController.navigate("shop") },
                onNavigateToCart = { navController.navigate("cart") },
                onNavigateToProfile = { navController.navigate("profile") },
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                navController = navController,
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToShop = { navController.navigate("shop") },
                onNavigateToFavorites = { navController.navigate("favorites") },
                onNavigateToCart = { navController.navigate("cart") },
            )
        }

        composable("add_address") {
            AddAddressScreen(
                navController = navController
            )
        }

        // New route for editing an address
        composable(
            "edit_address/{addressId}",
            arguments = listOf(navArgument("addressId") { type = NavType.StringType })
        ) { backStackEntry ->
            val addressId = backStackEntry.arguments?.getString("addressId") ?: ""
            EditAddressScreen(
                navController = navController,
                addressId = addressId
            )
        }

        // Add this to your existing NavHost routes in AppNavigation.kt
        composable("address_list") {
            AddressListScreen(
                navController = navController
            )
        }

    }
}

