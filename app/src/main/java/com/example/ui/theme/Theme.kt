package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBg,
    secondary = DigitalPurple,
    onSecondary = SkyWhiteText,
    tertiary = AlertOrange,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = SkyWhiteText,
    surface = MidnightSurface,
    onSurface = SkyWhiteText,
    surfaceVariant = MidnightSurface,
    onSurfaceVariant = DarkGreyText
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricTeal,
    onPrimary = LightSurface,
    secondary = RoyalPurple,
    onSecondary = CharcoalText,
    tertiary = DarkAmber,
    onTertiary = LightSurface,
    background = LightBg,
    onBackground = CharcoalText,
    surface = LightSurface,
    onSurface = CharcoalText,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightGreyText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors by default to preserve specialized Quantum branding
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
