package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.*
import com.trishit.egloo.domain.models.*
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorBrainRepository(private val client: HttpClient) : BrainRepository {
    override fun getBrainToday(): Flow<BrainToday> = flow {
        try {
            val response = client.get("/api/v1/brain/today")
            response.safeParse<BrainTodayResponse>().onSuccess { dto ->
                emit(dto.toDomain())
            }
        } catch (_: Exception) {}
    }

    override fun getBrainMissing(): Flow<BrainMissing> = flow {
        try {
            val response = client.get("/api/v1/brain/missing")
            response.safeParse<BrainMissingResponse>().onSuccess { dto ->
                emit(dto.toDomain())
            }
        } catch (_: Exception) {}
    }

    override fun getBrainConnections(): Flow<List<BrainConnection>> = flow {
        try {
            val response = client.get("/api/v1/brain/connections")
            response.safeParse<BrainConnectionsResponse>().onSuccess { dto ->
                emit(dto.connections.map { it.toDomain() })
            }
        } catch (_: Exception) {}
    }

    override fun getBrainAlerts(): Flow<List<BrainAlert>> = flow {
        try {
            val response = client.get("/api/v1/brain/alerts")
            response.safeParse<List<BrainAlertDto>>().onSuccess { alerts ->
                emit(alerts.map { it.toDomain() })
            }
        } catch (_: Exception) {}
    }

    override suspend fun clearAlerts(): Result<Unit> {
        return try {
            val response = client.delete("/api/v1/brain/alerts")
            response.safeParse<Unit>().map { Unit }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
