package com.trishit.egloo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.trishit.egloo.di.eglooModule
import com.trishit.egloo.navigation.DefaultRootComponent
import com.trishit.egloo.navigation.RootContent
import org.koin.core.context.startKoin
import web.storage.localStorage

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin { modules(eglooModule) }
    val lifecycle = LifecycleRegistry()
    
    val root = DefaultRootComponent(
        componentContext = DefaultComponentContext(lifecycle),
        isFirstLaunch = wasmIsFirstLaunch(),
        onOnboardingComplete = {
            localStorage.setItem("egloo_onboarding_done", "true")
        }
    )
    
    lifecycle.resume()

    ComposeViewport(viewportContainerId = "compose-root") {
        RootContent(component = root)
    }
}

private fun wasmIsFirstLaunch(): Boolean {
    return localStorage.getItem("egloo_onboarding_done") == null
}
