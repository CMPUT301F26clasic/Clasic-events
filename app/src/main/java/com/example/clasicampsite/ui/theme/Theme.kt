/**
 * Purpose: The app's Material theme. Picks the light, dark or colour blind colour scheme.
 * Outstanding issues: None.
 */
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

/**
 * builds a color blind scheme on the same backgrounds as above. Only the button (primary)
 * and error colors change, picked so each type of color blindness can tell them apart.
 */
private fun colorBlindScheme(darkTheme: Boolean, primary: Color, error: Color) =
    if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.Black,
            secondary = primary,
            tertiary = primary,
            error = error,
            background = Color(0xFF1C1B1F),
            surface = Color(0xFF1C1B1F),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = primary,
            tertiary = primary,
            error = error,
            background = Color.White,
            surface = Color.White,
            onBackground = Color(0xFF1C1B1F),
            onSurface = Color(0xFF1C1B1F)
        )
    }

/**
 * The app's theme. It picks the colours for the chosen colour blind mode.
 *
 * @param darkTheme whether the phone is in dark mode
 * @param dynamicColor whether to use the phone's wallpaper colours on Android 12+
 * @param colorMode the colour blind mode picked on the Profile screen
 * @param content the screens shown inside the theme
 */
@Composable
fun ClasiCampsiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    /** color blind mode picked on the Profile screen */
    colorMode: String = "regular",
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        /** red-blind people see reds as dark, so errors are orange instead */
        colorMode == "protanopia" ->
            if (darkTheme) colorBlindScheme(true, ColorBlindSkyBlue, ColorBlindOrange)
            else colorBlindScheme(false, ColorBlindBlue, ColorBlindAmber)
        /** for green-blind people, blue and orange-red are the easiest pair to tell apart */
        colorMode == "deuteranopia" ->
            if (darkTheme) colorBlindScheme(true, ColorBlindSkyBlue, ColorBlindVermillion)
            else colorBlindScheme(false, ColorBlindBlue, ColorBlindVermillion)
        /** blue-blind people mix up blue/green but see red well, so teal and red */
        colorMode == "tritanopia" ->
            if (darkTheme) colorBlindScheme(true, TritanTealLight, TritanRedLight)
            else colorBlindScheme(false, TritanTeal, TritanRed)
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