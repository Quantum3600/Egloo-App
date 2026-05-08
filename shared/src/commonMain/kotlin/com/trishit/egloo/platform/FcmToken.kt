package com.trishit.egloo.platform

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic FCM token management.
 * Returns the current FCM token if available, or null if not yet obtained.
 * Emits new tokens when the token is refreshed.
 */
expect fun getFcmToken(): Flow<String?>

/**
 * Store the FCM token locally for retrieval.
 * This is called by platform-specific listeners when the token is refreshed.
 */
expect suspend fun storeFcmToken(token: String)

/**
 * Get the last stored FCM token (synchronous).
 * Useful for immediate access without Flow.
 */
expect suspend fun getStoredFcmToken(): String?

