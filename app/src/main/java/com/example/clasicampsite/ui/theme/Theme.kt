package com.example.clasicampsite.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.White,
    surface = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

// color blind mode, same backgrounds as above but blue/orange instead of purple/red
private val ColorBlindDarkScheme = darkColorScheme(
    primary = ColorBlindSkyBlue,
    onPrimary = Color.Black,
    secondary = ColorBlindOrange,
    tertiary = ColorBlindYellow,
    error = ColorBlindVermillion,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onBackground = Color.White,
    onSurface = Color.White
)

private val ColorBlindLightScheme = lightColorScheme(
    primary = ColorBlindBlue,
    onPrimary = Color.White,
    secondary = ColorBlindOrange,
    tertiary = ColorBlindSkyBlue,
    error = ColorBlindVermillion,
    background = Color.White,
    surface = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun ClasiCampsiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    colorBlind: Boolean = false, // color blind mode, turned on from the Profile screen
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        colorBlind && darkTheme -> ColorBlindDarkScheme
        colorBlind -> ColorBlindLightScheme
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