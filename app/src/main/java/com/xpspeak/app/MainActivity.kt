package com.xpspeak.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xpspeak.app.core.navigation.XPSpeakNavHost
import com.xpspeak.app.core.ui.theme.XPSpeakTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XPSpeakTheme {
                XPSpeakNavHost()
            }
        }
    }
}