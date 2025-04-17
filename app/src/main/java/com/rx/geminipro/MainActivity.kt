package com.rx.geminipro

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rx.geminipro.screens.GeminiViewModel
import com.rx.geminipro.ui.theme.GeminiProTheme
import com.rx.geminipro.utils.AndroidConnectivityObserver
import com.rx.geminipro.utils.ConnectivityViewModel
import com.rx.geminipro.screens.GeminiViewer
import com.rx.geminipro.utils.NavigationMode
import com.rx.geminipro.utils.ThemePreferenceManager
import com.rx.geminipro.utils.getSystemNavigationMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val geminiViewModel by viewModels<GeminiViewModel>()

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                !geminiViewModel.isReady.value
            }
            setOnExitAnimationListener { screen ->
                screen.remove()
            }
        }

        enableEdgeToEdge()

        setContent {
            val isSystemDark = isSystemInDarkTheme()
            val themePreferenceManager = ThemePreferenceManager(this)
            val isDarkTheme by themePreferenceManager.themePreferenceFlow.collectAsState(initial = isSystemDark)

            GeminiProTheme(darkTheme = isDarkTheme) {
                val connectivityViewModel = viewModel<ConnectivityViewModel> {
                    ConnectivityViewModel(
                        connectivityObserver = AndroidConnectivityObserver(
                            context = applicationContext
                        )
                    )
                }
                val isConnected by connectivityViewModel.isConnected.collectAsStateWithLifecycle()

                if(geminiViewModel.keepScreenOn.value)
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                val rootView = findViewById<View>(android.R.id.content)
                val navMode = getSystemNavigationMode(rootView)

                if(navMode == NavigationMode.GESTURAL)
                    GeminiViewer(isConnected, applicationContext, geminiViewModel, themePreferenceManager, Modifier.statusBarsPadding())
                else
                    Scaffold { innerPadding ->
                        GeminiViewer(isConnected, applicationContext, geminiViewModel, themePreferenceManager, Modifier.padding(innerPadding))
                    }
            }
        }
    }
}