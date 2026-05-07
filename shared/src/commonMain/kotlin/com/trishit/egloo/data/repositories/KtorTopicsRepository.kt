package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.TopicListResponse
import com.trishit.egloo.data.api.TopicResponse
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.domain.models.Topic
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

@Serializable
private data class CreateTopicRequest(val name: String, val summary: String)

class KtorTopicsRepository(private val client: HttpClient) : TopicsRepository {
    override fun getTopics(): Flow<List<Topic>> = flow {
        try {
            val response = client.get("/api/v1/topics")
            if (response.status.value == 200) {
                val listResponse = response.body<TopicListResponse>()
                emit(listResponse.topics.map { it.toDomain() })
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getTopicById(id: String): Flow<Topic?> = flow {
        try {
            val response = client.get("/api/v1/topics/$id")
            if (response.status.value == 200) {
                val dto = response.body<TopicResponse>()
                emit(dto.toDomain())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }

    override suspend fun createTopic(name: String, summary: String): Result<Unit> {
        return try {
            val response = client.post("/api/v1/topics") {
                contentType(ContentType.Application.Json)
                setBody(CreateTopicRequest(name, summary))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to create topic: ${response.status}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun triggerTopicGeneration(): Result<Unit> {
        return try {
            val response = client.post("/api/v1/ingest/trigger-all")
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to trigger generation: ${response.status}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
