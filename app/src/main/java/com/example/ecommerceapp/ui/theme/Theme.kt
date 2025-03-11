package com.example.ecommerceapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*


/*SHADES (Base blue + black):

Color(0xFF1976D2) // Medium blue
Color(0xFF0D47A1) // Darker blue
Color(0xFF052C65) // Very dark blue
Color(0xFF021C42) // Nearly black-blue
Color(0xFF00102A) // Almost black

TONES (Base blue + grey):

Color(0xFF1976D2) // Same base blue
Color(0xFF4B89C8) // Blue-grey
Color(0xFF789DC0) // More grey-blue
Color(0xFF94A7B8) // Grey with blue tint
Color(0xFFA9B2BC) // Mostly grey

TINTS (Base blue + white):

Color(0xFF1976D2) // Same base blue
Color(0xFF57A5E5) // Lighter blue
Color(0xFF7FD4F7) // Light blue
Color(0xFFA6E6F7) // Very light blue
Color(0xFFD9F6FB) // Almost white blue*/

// Define your custom colors
val LightPrimary = Color(0xFF1976D2)
val LightPrimaryVariant = Color(0xFF4B89C8)
val LightSecondary = Color(0xFF789DC0)
val LightBackground = Color(0xFFFFFFFF) // White background for light mode

val DarkPrimary = Color(0xFFD9F6FB)
val DarkPrimaryVariant = Color(0xFF4B89C8)
val DarkSecondary = Color(0xFF789DC0)
val DarkBackground = Color(0xFF121212) // Dark background for dark mode

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = DarkPrimaryVariant,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkBackground,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    primaryContainer = LightPrimaryVariant,
    secondary = LightSecondary,
    background = LightBackground,
    surface = LightBackground,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun EcommerceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}