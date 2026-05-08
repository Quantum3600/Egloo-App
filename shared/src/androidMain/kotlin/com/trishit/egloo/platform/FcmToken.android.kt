package com.trishit.egloo.platform

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.core.content.edit

/**
 * Android implementation of FCM token management.
 * Uses SharedPreferences to persist the token locally.
 */
// Reuse the shared application context provided in PlatformOpenUrl.android.kt
// MainActivity sets `androidAppContext = applicationContext` on startup.
// Use that same variable here to access SharedPreferences.

private fun getFcmPrefs(): SharedPreferences {
    return androidAppContext.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
}

private const val FCM_TOKEN_KEY = "fcm_token"

// Backing StateFlow which emits the current token and updates when it's changed.
private val _tokenState = MutableStateFlow<String?>(null)

private var initialized = false

private fun ensureInitialized() {
    if (initialized) return
    initialized = true
    // Try to load existing token from preferences. If androidAppContext is not yet set,
    // this will throw; callers should ensure platform initialization (MainActivity sets context).
    try {
        val existing = getFcmPrefs().getString(FCM_TOKEN_KEY, null)
        _tokenState.value = existing
    } catch (_: Throwable) {
        // If context isn't set yet, leave value as null. It will be populated on first store.
    }
}

actual fun getFcmToken(): Flow<String?> {
    ensureInitialized()
    return _tokenState
}

actual suspend fun storeFcmToken(token: String) {
    // Persist and emit new value
    try {
        getFcmPrefs().edit { putString(FCM_TOKEN_KEY, token) }
    } catch (_: Throwable) {
        // ignore write failures for now
    }
    _tokenState.value = token
}

actual suspend fun getStoredFcmToken(): String? {
    return try { getFcmPrefs().getString(FCM_TOKEN_KEY, null) } catch (_: Throwable) { null }
}


