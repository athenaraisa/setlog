package com.example.quicksnapdiary

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.example.quicksnapdiary.ui.AppNav

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFD5FF3F),
                    onPrimary = Color(0xFF171912),
                    background = Color(0xFF10110F),
                    surface = Color(0xFF1A1C18),
                    secondary = Color(0xFFFF8066)
                )
            ) {
                AppNav(rememberNavController())
            }
        }
    }
}
