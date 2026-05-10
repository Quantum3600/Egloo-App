package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.*
import com.trishit.egloo.domain.models.*
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorIngestRepository(private val client: HttpClient) : IngestRepository {
    override fun getRecentJobs(): Flow<List<IngestJob>> = flow {
        try {
            val response = client.get("/api/v1/ingest/jobs")
            response.safeParse<JobListResponse>().onSuccess { wrapped ->
                emit(wrapped.jobs.map { it.toDomain() })
            }
        } catch (_: Exception) {}
    }

    override fun getJobStatus(jobId: String): Flow<IngestJob> = flow {
        try {
            val response = client.get("/api/v1/ingest/job/$jobId")
            response.safeParse<JobStatusResponse>().onSuccess { job ->
                emit(job.toDomain())
            }
        } catch (_: Exception) {}
    }

    override suspend fun triggerIngest(sourceId: String): Result<String> {
        // IMPORTANT: sourceId MUST be the UUID from /api/v1/sources
        println("KtorIngestRepository: Triggering ingest for UUID: $sourceId")
        return try {
            val response = client.post("/api/v1/ingest/trigger/$sourceId")
            response.safeParse<IngestResponse>().map { it.job_id }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun triggerAllIngest(): Result<List<String>> {
        return try {
            val response = client.post("/api/v1/ingest/trigger-all")
            response.safeParse<List<IngestResponse>>().map { it.map { job -> job.job_id } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
