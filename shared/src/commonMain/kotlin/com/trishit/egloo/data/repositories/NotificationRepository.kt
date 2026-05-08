package com.trishit.egloo.data.repositories

import com.trishit.egloo.platform.getFcmToken
import com.trishit.egloo.platform.getStoredFcmToken
import com.trishit.egloo.platform.storeFcmToken
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
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
 * Real implementation using Ktor to sync tokens and preferences with backend.
 */
class KtorNotificationRepository(
    private val client: HttpClient
) : NotificationRepository {
    override fun getFcmTokenStream(): Flow<String?> = getFcmToken()

    override suspend fun getToken(): String? = getStoredFcmToken()

    override suspend fun registerToken(token: String): Result<Unit> {
        return try {
            val response = client.post("/api/v1/notifications/register") {
                contentType(ContentType.Application.Json)
                setBody(com.trishit.egloo.data.api.NotificationRegisterRequest(fcm_token = token))
            }
            if (response.status.isSuccess()) {
                storeFcmToken(token)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to register token: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unregisterToken(): Result<Unit> {
        return try {
            val response = client.delete("/api/v1/notifications/unregister")
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unregister token: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isDigestNotificationEnabled(): Boolean {
        return try {
            val response = client.get("/api/v1/settings")
            if (response.status.isSuccess()) {
                val settings = response.body<com.trishit.egloo.data.api.AppSettingsDto>()
                settings.digest_notifications_enabled
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }

    override suspend fun setDigestNotificationEnabled(enabled: Boolean): Result<Unit> {
        return try {
            val response = client.put("/api/v1/settings") {
                contentType(ContentType.Application.Json)
                // We fetch current settings first or send partial update if supported
                // For now, assume simple toggle on /notifications/preferences if it existed
                // but based on API guidelines, it's under /settings
                val currentResponse = client.get("/api/v1/settings")
                if (currentResponse.status.isSuccess()) {
                    val current = currentResponse.body<com.trishit.egloo.data.api.AppSettingsDto>()
                    val updated = current.copy(digest_notifications_enabled = enabled)
                    client.put("/api/v1/settings") {
                        contentType(ContentType.Application.Json)
                        setBody(updated)
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to fetch settings"))
                }
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to update preferences"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
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

