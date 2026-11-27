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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // load this device's saved color blind setting once when the app opens
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