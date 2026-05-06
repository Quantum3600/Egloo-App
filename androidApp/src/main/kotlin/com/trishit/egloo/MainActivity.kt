package com.trishit.egloo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.defaultComponentContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import com.trishit.egloo.navigation.DefaultRootComponent
import com.trishit.egloo.navigation.RootContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val root = DefaultRootComponent(
            componentContext = defaultComponentContext(),
            isFirstLaunch = isFirstLaunch(),
            onOnboardingComplete = { markOnboardingDone() }
        )
        setContent {
            org.koin.compose.KoinContext {
                RootContent(component = root)
            }
        }
    }

    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("egloo_prefs", MODE_PRIVATE)
        return !prefs.getBoolean("onboarding_done", false)
    }

    private fun markOnboardingDone() {
        getSharedPreferences("egloo_prefs", MODE_PRIVATE).edit {
            putBoolean("onboarding_done", true)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    RootContent(
        component = DefaultRootComponent(
            componentContext = com.arkivanov.decompose.DefaultComponentContext(com.arkivanov.essenty.lifecycle.LifecycleRegistry()),
            isFirstLaunch = false
        )
    )
}