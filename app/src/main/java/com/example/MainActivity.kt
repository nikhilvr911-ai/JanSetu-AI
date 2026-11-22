package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.config.configureAppCheck
import com.example.ui.JanSetuScreen
import com.example.ui.theme.JanSetuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable edge-to-edge with light icons over the deep indigo civic header
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // STEP 3: Render JanSetu AI Compose UI
        setContent {
            JanSetuTheme {
                JanSetuScreen()
            }
        }
    }
}
