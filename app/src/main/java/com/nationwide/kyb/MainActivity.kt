package com.nationwide.kyb

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.nationwide.kyb.core.navigation.KybNavigation
import com.nationwide.kyb.ui.theme.KybriskradarcomposeTheme
import com.nationwide.kyb.ui.theme.PrimaryNavy
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Set status bar color to match app bar
        window.statusBarColor = PrimaryNavy.hashCode()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
            }
        }
        
        // Load mock data.json from assets
        val assetsInputStream = try {
            assets.open("data.json")
        } catch (e: IOException) {
            // Fallback: try to load from mock folder if assets doesn't exist
            // In production, this would come from a proper data source
            null
        }
        
        if (assetsInputStream == null) {
            // Create a placeholder input stream or handle error
            // For now, we'll show an error
            setContent {
                KybriskradarcomposeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        androidx.compose.material3.Text("Error: data.json not found in assets")
                    }
                }
            }
            return
        }
        
        setContent {
            KybriskradarcomposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    KybNavigation(
                        navController = navController,
                        assetsInputStream = assetsInputStream,
                        context = this
                    )
                }
            }
        }
    }
}