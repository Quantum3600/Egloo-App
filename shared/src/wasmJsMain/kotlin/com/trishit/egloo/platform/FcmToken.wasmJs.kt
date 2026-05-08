package com.trishit.egloo.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Web (Wasm) implementation of FCM token management.
 * FCM tokens on web are managed via Firebase Web SDK.
 * This is a stub; full implementation requires firebase-js-sdk initialization.
 */
actual fun getFcmToken(): Flow<String?> = flowOf(null)

actual suspend fun storeFcmToken(token: String) {
    // TODO: Store token in browser localStorage or IndexedDB
}

actual suspend fun getStoredFcmToken(): String? = null

