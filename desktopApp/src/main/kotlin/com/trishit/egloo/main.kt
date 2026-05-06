package com.trishit.egloo

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.mayakapps.compose.windowstyler.WindowCornerPreference
import com.mayakapps.compose.windowstyler.WindowFrameStyle
import com.mayakapps.compose.windowstyler.WindowStyle
import com.trishit.egloo.data.repositories.SettingsRepository
import com.trishit.egloo.di.eglooModule
import com.trishit.egloo.navigation.DefaultRootComponent
import com.trishit.egloo.navigation.RootContent
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import java.util.prefs.Preferences

fun main() = application {
    // ── DI ────────────────────────────────────────────────────────────────────
    startKoin { modules(eglooModule) }

    // ── Decompose ─────────────────────────────────────────────────────────────
    val lifecycle = LifecycleRegistry()
    val root = remember {
        DefaultRootComponent(
            componentContext = DefaultComponentContext(lifecycle),
            isFirstLaunch = isFirstLaunch(),
            onOnboardingComplete = { markOnboardingDone() }
        )
    }

    // ── Window state ──────────────────────────────────────────────────────────
    val windowState = rememberWindowState(
        width  = 1200.dp,
        height = 800.dp,
    )

    Window(
        onCloseRequest = ::exitApplication,
        state          = windowState,
        title          = "Egloo",
    ) {
        KoinContext {
            val settingsRepo = koinInject<SettingsRepository>()
            val settings by settingsRepo.getSettings().collectAsState(initial = null)
            val isDarkTheme = settings?.darkTheme ?: false // Default to dark

            WindowStyle(
                isDarkTheme = isDarkTheme,
                frameStyle = WindowFrameStyle(
                    titleBarColor = if(isDarkTheme) Color(30, 47, 66) else Color(225, 245, 238),
                    cornerPreference = WindowCornerPreference.ROUNDED
                ) // Use default frame but with theme awareness
            )
            RootContent(component = root)
        }
    }
}

private fun isFirstLaunch(): Boolean {
    val prefs = Preferences.userRoot().node("com/egloo")
    return !prefs.getBoolean("onboarding_done", false)
}

private fun markOnboardingDone() {
    val prefs = Preferences.userRoot().node("com/egloo")
    prefs.putBoolean("onboarding_done", true)
    prefs.flush()
}
