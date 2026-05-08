package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.*
import com.trishit.egloo.domain.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorBrainRepository(private val client: HttpClient) : BrainRepository {
    override fun getBrainToday(): Flow<BrainToday> = flow {
        try {
            val response = client.get("/api/v1/brain/today")
            if (response.status.value == 200) {
                emit(response.body<BrainTodayResponse>().toDomain())
            }
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    override fun getBrainMissing(): Flow<BrainMissing> = flow {
        try {
            val response = client.get("/api/v1/brain/missing")
            if (response.status.value == 200) {
                emit(response.body<BrainMissingResponse>().toDomain())
            }
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    override fun getBrainConnections(): Flow<List<BrainConnection>> = flow {
        try {
            val response = client.get("/api/v1/brain/connections")
            if (response.status.value == 200) {
                val dto = response.body<BrainConnectionsResponse>()
                emit(dto.connections.map { it.toDomain() })
            }
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    override fun getBrainAlerts(): Flow<List<BrainAlert>> = flow {
        try {
            val response = client.get("/api/v1/brain/alerts")
            if (response.status.value == 200) {
                val alerts = response.body<List<BrainAlertDto>>()
                emit(alerts.map { it.toDomain() })
            }
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    override suspend fun clearAlerts(): Result<Unit> {
        return try {
            val response = client.delete("/api/v1/brain/alerts")
            if (response.status.value == 200) Result.success(Unit)
            else Result.failure(Exception("Failed to clear alerts"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
