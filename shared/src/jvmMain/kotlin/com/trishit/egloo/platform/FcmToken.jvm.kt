package com.trishit.egloo.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Desktop (JVM) implementation of FCM token management.
 * FCM is not supported on desktop; returns null.
 */
actual fun getFcmToken(): Flow<String?> = flowOf(null)

actual suspend fun storeFcmToken(token: String) {
    // Desktop doesn't support FCM
}

actual suspend fun getStoredFcmToken(): String? = null

