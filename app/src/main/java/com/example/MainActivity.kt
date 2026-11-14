package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LinguaBridgeApp
import com.example.ui.MainViewModel
import com.example.ui.theme.LinguaBridgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.firebase.configureAppCheck(this, intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val appContainer = (application as LinguaBridgeApplication).appContainer
        setContent {
            LinguaBridgeTheme {
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModel.factory(appContainer)
                )
                LinguaBridgeApp(viewModel = mainViewModel)
            }
        }
    }
}
