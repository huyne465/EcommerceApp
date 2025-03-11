package com.example.ecommerceapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ecommerceapp.core.AppNavigation
import com.example.ecommerceapp.presentation.Repo.UserPreferencesRepository
import com.example.ecommerceapp.ui.theme.EcommerceAppTheme
import vn.zalopay.sdk.ZaloPaySDK;
import vn.zalopay.sdk.Environment;

class MainActivity : ComponentActivity() {
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize UserPreferencesRepository
        userPreferencesRepository = UserPreferencesRepository(applicationContext)

        // Initialize ZaloPaySDK with your App ID from AppInfo
        ZaloPaySDK.init(553, Environment.SANDBOX)


        setContent {
            // Use the repository's dark mode state
            val isDarkMode by userPreferencesRepository.isDarkMode.collectAsState(initial = false)

            EcommerceAppTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        Modifier.padding(innerPadding),
                        userPreferencesRepository
                    )
                }
            }
        }
    }

    // Handle deep link callback from ZaloPay app
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ZaloPaySDK.getInstance().onResult(intent)
    }


}

