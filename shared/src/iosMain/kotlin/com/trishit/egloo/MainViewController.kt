package com.trishit.egloo

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.trishit.egloo.di.eglooModule
import com.trishit.egloo.navigation.DefaultRootComponent
import com.trishit.egloo.navigation.RootContent
import platform.UIKit.UIViewController

import com.trishit.egloo.data.repositories.AuthRepository
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform.getKoin

fun MainViewController(): UIViewController {
    // DI
    try {
        startKoin { modules(eglooModule) }
    } catch (e: Exception) {
        // Already started
    }

    val authRepo: AuthRepository = getKoin().get()

    // Decompose root
    val root = DefaultRootComponent(
        componentContext = DefaultComponentContext(ApplicationLifecycle()),
        isFirstLaunch = iosIsFirstLaunch(),
        isAuthenticated = authRepo.getToken() != null,
    )

    return ComposeUIViewController {
        RootContent(component = root)
    }
}

private fun iosIsFirstLaunch(): Boolean {
    // NSUserDefaults via Kotlin/Native
    val defaults = platform.Foundation.NSUserDefaults.standardUserDefaults
    return if (defaults.boolForKey("onboarding_done")) {
        false
    } else {
        defaults.setBool(true, forKey = "onboarding_done")
        true
    }
}
