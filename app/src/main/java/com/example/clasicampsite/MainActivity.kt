/**
 * Purpose: The app's only activity. Loads the saved colour blind setting and shows the app
 * inside the theme.
 * Outstanding issues: None.
 */
package com.example.clasicampsite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.clasicampsite.ui.theme.ClasiCampsiteTheme

/** The app's only activity. It shows the whole app inside the theme. */
class MainActivity : ComponentActivity() {
    /**
     * Loads the saved colour blind mode and shows the app.
     *
     * @param savedInstanceState saved state from Android, not used
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            /** load this device's saved color blind setting once when the app opens */
            LaunchedEffect(Unit) {
                ColorBlindMode.load(this@MainActivity)
            }
            ClasiCampsiteTheme(colorMode = ColorBlindMode.mode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    clasiCampsiteApp()
                }
            }
        }
    }
}