package com.trishit.egloo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.defaultComponentContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.trishit.egloo.navigation.DefaultRootComponent
import com.trishit.egloo.navigation.RootContent

import com.trishit.egloo.data.repositories.AuthRepository
import com.trishit.egloo.platform.DeepLinkHandler
import com.trishit.egloo.platform.androidAppContext
import com.trishit.egloo.platform.AndroidFilePicker
import org.koin.android.ext.android.get
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        // Initialize Android file picker bridge BEFORE super.onCreate
        AndroidFilePicker.init(this)
        super.onCreate(savedInstanceState)

        // Provide application context for platform helpers
        androidAppContext = applicationContext

        handleIntent(intent)

        val authRepo: AuthRepository = get()
        val root = DefaultRootComponent(
            componentContext = defaultComponentContext(),
            isFirstLaunch = isFirstLaunch(),
            isAuthenticated = authRepo.getToken() != null,
            onOnboardingComplete = { markOnboardingDone() }
        )
        setContent {
            org.koin.compose.KoinContext {
                RootContent(component = root)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "egloo" && uri.host == "auth") {
            val status = uri.getQueryParameter("status")
            val source = uri.getQueryParameter("source")
            
            lifecycleScope.launch {
                DeepLinkHandler.emitAuthResult(status, source)
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