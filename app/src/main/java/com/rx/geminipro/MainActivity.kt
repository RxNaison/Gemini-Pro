package com.rx.geminipro

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rx.geminipro.screens.GeminiViewModel
import com.rx.geminipro.ui.theme.GeminiProTheme
import com.rx.geminipro.utils.AndroidConnectivityObserver
import com.rx.geminipro.utils.ConnectivityViewModel
import com.rx.geminipro.screens.GeminiViewer
import com.rx.geminipro.utils.ThemePreferenceManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val geminiViewModel by viewModels<GeminiViewModel>()

        installSplashScreen().apply {
            setKeepOnScreenCondition{
                !geminiViewModel.isReady.value
            }
            setOnExitAnimationListener{ screen->
                val zoomX = ObjectAnimator.ofFloat(
                    screen.iconView,
                    View.SCALE_X,
                    0.4f,
                    0.0f
                ).apply {
                    interpolator = OvershootInterpolator()
                    duration = 500L
                }

                val zoomY = ObjectAnimator.ofFloat(
                    screen.iconView,
                    View.SCALE_Y,
                    0.4f,
                    0.0f
                ).apply {
                    interpolator = OvershootInterpolator()
                    duration = 500L
                }

                val animatorSet = AnimatorSet().apply {
                    playTogether(zoomX, zoomY)
                    doOnEnd { screen.remove() }
                }

                animatorSet.start()
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

                GeminiViewer(isConnected, applicationContext, geminiViewModel, themePreferenceManager)
            }
        }
    }
}