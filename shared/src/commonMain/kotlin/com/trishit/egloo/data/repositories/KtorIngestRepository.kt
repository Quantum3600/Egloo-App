package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.*
import com.trishit.egloo.domain.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorIngestRepository(private val client: HttpClient) : IngestRepository {
    override fun getRecentJobs(): Flow<List<IngestJob>> = flow {
        try {
            val response = client.get("/api/v1/ingest/jobs")
            if (response.status.value == 200) {
                val jobs = response.body<List<JobStatusResponse>>()
                emit(jobs.map { it.toDomain() })
            }
        } catch (_: Exception) {
            // Log or handle error
        }
    }

    override fun getJobStatus(jobId: String): Flow<IngestJob> = flow {
        try {
            val response = client.get("/api/v1/ingest/job/$jobId")
            if (response.status.value == 200) {
                emit(response.body<JobStatusResponse>().toDomain())
            }
        } catch (_: Exception) {
            // Log or handle error
        }
    }

    override suspend fun triggerIngest(sourceId: String): Result<String> {
        return try {
            val response = client.post("/api/v1/ingest/trigger/$sourceId")
            // Accept both 200 OK and 202 Accepted for async operations
            if (response.status.value in listOf(200, 202)) {
                try {
                    val dto = response.body<IngestResponse>()
                    Result.success(dto.job_id)
                } catch (_: Exception) {
                    // Sometimes it might return a list even for single trigger
                    val dtos = response.body<List<IngestResponse>>()
                    Result.success(dtos.first().job_id)
                }
            } else {
                Result.failure(Exception("Failed to trigger ingest: ${response.status}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to trigger ingest: ${e.message}"))
        }
    }

    override suspend fun triggerAllIngest(): Result<List<String>> {
        return try {
            val response = client.post("/api/v1/ingest/trigger-all")
            // Accept both 200 OK and 202 Accepted for async operations
            if (response.status.value in listOf(200, 202)) {
                try {
                    val dtos = response.body<List<IngestResponse>>()
                    Result.success(dtos.map { it.job_id })
                } catch (_: Exception) {
                    // Fallback if it returns a single object instead of a list
                    val dto = response.body<IngestResponse>()
                    Result.success(listOf(dto.job_id))
                }
            } else {
                Result.failure(Exception("Failed to trigger all ingest: ${response.status}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to trigger all ingest: ${e.message}"))
        }
    }
}
