package com.trishit.egloo.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * JS implementation (browser) of FCM token management.
 * This is a minimal stub — full support requires integrating the Firebase Web SDK
 * via JS interop and storing tokens in localStorage or IndexedDB.
 */
actual fun getFcmToken(): Flow<String?> = flowOf(null)

actual suspend fun storeFcmToken(token: String) {
    // TODO: persist token via browser storage (localStorage / IndexedDB) or JS SDK
}

actual suspend fun getStoredFcmToken(): String? = null

