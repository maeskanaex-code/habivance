package app.habivance

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import app.habivance.data.lock.LockManager
import app.habivance.data.settings.LockRepository
import app.habivance.ui.lock.LockScreen
import app.habivance.ui.navigation.HabivanceNavHost
import app.habivance.ui.settings.SettingsViewModel
import app.habivance.ui.splash.BrandedSplashScreen
import app.habivance.ui.theme.HabivanceTheme
import app.habivance.ui.welcome.WelcomeScreen
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabivanceApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            val app = applicationContext as HabitApp
            MainScope().launch {
                app.container.lockRepository.setLastBackgroundedAt(0L)
            }
        }
    }
}

@Composable
private fun HabivanceApp() {
    val context = LocalContext.current
    val app = context.applicationContext as HabitApp
    val lockRepository: LockRepository = app.container.lockRepository

    val settingsViewModel: SettingsViewModel = viewModel()
    val themeMode by settingsViewModel.themeMode.collectAsState()

    // Splash state — true until animation finishes
    var showBrandedSplash by remember { mutableStateOf(true) }

    var isLocked by remember { mutableStateOf(false) }
    val lockEnabledFlow = lockRepository.lockEnabled

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    MainScope().launch {
                        lockRepository.setLastBackgroundedAt(System.currentTimeMillis())
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    MainScope().launch {
                        val should = LockManager.shouldLock(lockRepository, System.currentTimeMillis())
                        if (should) isLocked = true
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val enabled = lockEnabledFlow.first()
        if (enabled) {
            val should = LockManager.shouldLock(lockRepository, System.currentTimeMillis())
            isLocked = should
        }
    }

    val hasSeenWelcome by settingsViewModel.hasSeenWelcome.collectAsState()

    HabivanceTheme(themeMode = themeMode) {
        if (showBrandedSplash) {
            BrandedSplashScreen(
                onFinished = { showBrandedSplash = false }
            )
        } else {
            when (hasSeenWelcome) {
                null -> {
                    // Still loading preference — show nothing
                }
                false -> {
                    WelcomeScreen(
                        onGetStarted = { settingsViewModel.markWelcomeSeen() }
                    )
                }
                true -> {
                    if (isLocked) {
                        LockScreen(onUnlocked = { isLocked = false })
                    } else {
                        HabivanceNavHost(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
