package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.di.ServiceLocator
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WifiWiseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize dependency injection using ServiceLocator
        val factory = ServiceLocator.provideViewModelFactory(this)
        val viewModel = ViewModelProvider(this, factory)[WifiWiseViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}
