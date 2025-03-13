package com.example.ecommerceapp.presentation.order.greeting

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.ecommerceapp.R

@Composable
fun GreetingScreen(
    navController: NavHostController,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.greeting)) //get image
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(paddingValues)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            LottieAnimation(
                modifier = Modifier.size(400.dp),
                composition = composition,
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Thank You!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your order has been placed successfully.\nThank you for supporting our business!",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    // Navigate to home screen
                    navController.navigate("home") {
                        // Clear back stack so user can't go back to checkout flow
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Return to Home", fontSize = 16.sp)
            }
        }
    }
}