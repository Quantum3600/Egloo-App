package com.trishit.egloo.data.repositories

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

@Serializable
data class HealthStatus(
    val status: String, // "healthy", "unhealthy", "degraded"
    val services: Map<String, String>,
    val worker_status: String? = null
)

interface HealthRepository {
    fun getHealthStatus(): Flow<HealthStatus>
    fun getBrainHealth(): Flow<HealthStatus>
}

class KtorHealthRepository(private val client: HttpClient) : HealthRepository {
    override fun getHealthStatus(): Flow<HealthStatus> = flow {
        try {
            val response = client.get("/health")
            if (response.status.isSuccess()) {
                emit(response.body<HealthStatus>())
            } else {
                emit(HealthStatus("unhealthy", mapOf("error" to "Server returned ${response.status}")))
            }
        } catch (e: Exception) {
            emit(HealthStatus("unhealthy", mapOf("error" to (e.message ?: "Unknown error"))))
        }
    }

    override fun getBrainHealth(): Flow<HealthStatus> = flow {
        try {
            val response = client.get("/api/v1/brain/health")
            if (response.status.isSuccess()) {
                emit(response.body<HealthStatus>())
            } else {
                emit(HealthStatus("unhealthy", mapOf("error" to "Server returned ${response.status}")))
            }
        } catch (e: Exception) {
            emit(HealthStatus("unhealthy", mapOf("error" to (e.message ?: "Unknown error"))))
        }
    }
}
