package com.trishit.egloo.data.repositories

import com.trishit.egloo.platform.getFcmToken
import com.trishit.egloo.platform.getStoredFcmToken
import com.trishit.egloo.platform.storeFcmToken
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    /**
     * Stream of FCM tokens. Emits new token whenever it's refreshed.
     */
    fun getFcmTokenStream(): Flow<String?>

    /**
     * Get the current stored FCM token (synchronous).
     */
    suspend fun getToken(): String?

    /**
     * Register the FCM token with backend.
     * Called when token is obtained or refreshed.
     */
    suspend fun registerToken(token: String): Result<Unit>

    /**
     * Unregister the FCM token from backend.
     * Called on logout.
     */
    suspend fun unregisterToken(): Result<Unit>

    /**
     * Check if user has opted in to digest notifications.
     */
    suspend fun isDigestNotificationEnabled(): Boolean

    /**
     * Set digest notification preference.
     */
    suspend fun setDigestNotificationEnabled(enabled: Boolean): Result<Unit>
}

/**
 * In-memory implementation for development.
 */
class InMemoryNotificationRepository : NotificationRepository {
    private var currentToken: String? = null
    private var digestNotificationsEnabled = true

    override fun getFcmTokenStream(): Flow<String?> = getFcmToken()

    override suspend fun getToken(): String? = getStoredFcmToken()

    override suspend fun registerToken(token: String): Result<Unit> {
        return try {
            storeFcmToken(token)
            currentToken = token
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unregisterToken(): Result<Unit> {
        return try {
            currentToken = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isDigestNotificationEnabled(): Boolean = digestNotificationsEnabled

    override suspend fun setDigestNotificationEnabled(enabled: Boolean): Result<Unit> {
        return try {
            digestNotificationsEnabled = enabled
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

