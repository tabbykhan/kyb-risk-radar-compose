package com.nationwide.kyb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.compose.rememberNavController
import com.nationwide.kyb.core.navigation.KybNavigation
import com.nationwide.kyb.ui.theme.KybriskradarcomposeTheme
import com.nationwide.kyb.ui.theme.PrimaryNavy

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    window.statusBarColor = PrimaryNavy.toArgb()

    setContent {
        KybriskradarcomposeTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val navController = rememberNavController()

                KybNavigation(
                    navController = navController,
                    context = this
                )
            }
        }
    }
}
}