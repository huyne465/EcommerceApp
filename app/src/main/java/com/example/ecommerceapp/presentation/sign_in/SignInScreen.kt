package com.example.ecommerceapp.presentation.sign_in

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.ecommerceapp.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = viewModel(),
    navController: NavHostController,
) {


    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.welcome)) //get image

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Effect để xử lý điều hướng khi Login thành công
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.navigate("home") {
                // Xóa màn hình đăng ký khỏi back stack (tùy chọn)
                popUpTo("sign_in") { inclusive = true }
            }
            // Reset trạng thái sau khi đã điều hướng
            viewModel.resetSignInState()
        }
    }

    LaunchedEffect(key1 = uiState.hasUser) {
        if (uiState.hasUser) {
            navController.navigate("home") {
            }

            viewModel.resetSignInState()
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()  // Fill the entire screen
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center  // Center vertically
    ) {

        LottieAnimation(
            modifier = Modifier.size(320.dp),
            composition = composition,

            )

        Text(
            text = "LOGIN",
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(5.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = { viewModel.updateEmail(it) },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth(0.8f)  // Take 80% of screen width
                .padding(bottom = 16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.password,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(bottom = 16.dp),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        if (!uiState.errorMessage.isNullOrEmpty()) {
            Text(
                text = uiState.errorMessage!!,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            enabled = !uiState.isLoading,
            onClick = {
                viewModel.signIn(uiState.email, uiState.password)
                if (uiState.isSuccess){
                    navController.navigate("home")
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(bottom = 16.dp)
        ) {
            if(uiState.isLoading) {
                Text(text = "Loading...")
            } else {
                Text(text = "Sign in")
            }
        }

        TextButton(onClick = { navController.navigate("signUp") }) {
            Text("Don't have an account? Create an account")
        }

    }
}

@Preview
@Composable
private fun preview() {

}