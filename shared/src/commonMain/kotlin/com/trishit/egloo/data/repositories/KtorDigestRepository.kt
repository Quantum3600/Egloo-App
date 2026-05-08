package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.DigestResponse
import com.trishit.egloo.data.api.GenerateDigestRequest
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.platform.getStoredFcmToken
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorDigestRepository(private val client: HttpClient) : DigestRepository {
    override fun getDailyDigest(): Flow<DigestResult> = flow {
        emit(DigestResult.Loading)
        try {
            val response = client.get("/api/v1/digest/today")
            if (response.status.value == 200) {
                val dto = response.body<DigestResponse>()
                emit(DigestResult.Success(dto.toDomain()))
            } else {
                emit(DigestResult.Error("Failed to load digest: ${response.status}"))
            }
        } catch (e: Exception) {
            emit(DigestResult.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun generateDigest(force: Boolean): Result<Unit> {
        return try {
            // Include FCM token if available for push notification support
            val fcmToken = getStoredFcmToken()
            val response = client.post("/api/v1/digest/generate") {
                contentType(ContentType.Application.Json)
                setBody(GenerateDigestRequest(force_regenerate = force, fcm_token = fcmToken))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to generate digest: ${response.status}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveDigest(id: String): Result<Unit> {
        return try {
            val response = client.post("/api/v1/digest/$id/save")
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to save digest: ${response.status}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
