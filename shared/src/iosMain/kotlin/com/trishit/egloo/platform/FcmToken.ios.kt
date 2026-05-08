package com.trishit.egloo.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * iOS implementation of FCM token management.
 * For iOS, use Apple Push Notification (APNs) device token instead.
 * This is a stub; full implementation requires integrating with FirebaseMessaging SDK.
 */
actual fun getFcmToken(): Flow<String?> = flowOf(null)

actual suspend fun storeFcmToken(token: String) {
    // TODO: Store APNs device token using UserDefaults or Keychain
}

actual suspend fun getStoredFcmToken(): String? = null

