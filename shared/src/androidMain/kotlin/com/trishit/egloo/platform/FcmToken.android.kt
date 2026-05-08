package com.trishit.egloo.platform

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

/**
 * Android implementation of FCM token management.
 * Uses SharedPreferences to persist the token locally.
 */
internal lateinit var androidAppContextForFcm: Context

private fun getFcmPrefs(): SharedPreferences {
    return androidAppContextForFcm.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
}

private const val FCM_TOKEN_KEY = "fcm_token"

actual fun getFcmToken(): Flow<String?> {
    // Note: SharedPreferences changes are not observable via Flow in a simple way.
    // For now, return current token. A real implementation would use LiveData or StateFlow.
    val token = getFcmPrefs().getString(FCM_TOKEN_KEY, null)
    return flowOf(token)
}

actual suspend fun storeFcmToken(token: String) {
    getFcmPrefs().edit().putString(FCM_TOKEN_KEY, token).apply()
}

actual suspend fun getStoredFcmToken(): String? {
    return getFcmPrefs().getString(FCM_TOKEN_KEY, null)
}


