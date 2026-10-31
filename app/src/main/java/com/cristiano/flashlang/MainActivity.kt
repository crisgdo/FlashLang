package com.cristiano.flashlang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cristiano.flashlang.ui.theme.FlashLangTheme
import com.cristiano.flashlang.ui.navigation.FlashLangNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlashLangTheme {
                FlashLangNavHost((application as FlashLangApplication).deckRepository)
            }
        }
    }
}

