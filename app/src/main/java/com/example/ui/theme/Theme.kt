package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NetPulsePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF172554),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = NetPulseSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = NetPulseSuccess,
    onTertiary = Color(0xFF052E16),
    background = NetPulseDarkBackground,
    onBackground = NetPulseDarkOnBackground,
    surface = NetPulseDarkSurface,
    onSurface = NetPulseDarkOnBackground,
    surfaceVariant = NetPulseDarkCard,
    onSurfaceVariant = NetPulseDarkMuted,
    error = NetPulseError,
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA)
)

private val LightColorScheme = lightColorScheme(
    primary = NetPulsePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE8FF),
    onPrimaryContainer = Color(0xFF0A2E75),
    secondary = NetPulseSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE5FF),
    onSecondaryContainer = Color(0xFF35126D),
    tertiary = NetPulseSuccess,
    onTertiary = Color(0xFF003918),
    background = NetPulseBackground,
    onBackground = NetPulseOnBackground,
    surface = NetPulseSurface,
    onSurface = NetPulseOnBackground,
    surfaceVariant = NetPulseCard,
    onSurfaceVariant = NetPulseMuted,
    error = NetPulseError,
    onError = Color.White,
    errorContainer = Color(0xFFFFE0E0),
    onErrorContainer = Color(0xFF7A1010)
)

@Composable
fun MyApplicationTheme(
    // NetPulse intentionally starts in the professional light theme. Callers can still
    // opt into the dark Material 3 scheme when an appearance preference is added.
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
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
