package com.termoot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.termoot.ui.navigation.TermootNavGraph
import com.termoot.ui.theme.BackgroundDark
import com.termoot.ui.theme.TermootTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Make navigation bar fully transparent so our theme controls the colour
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            TermootTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    val navController = rememberNavController()
                    TermootNavGraph(navController = navController)
                }
            }
        }
    }
}
