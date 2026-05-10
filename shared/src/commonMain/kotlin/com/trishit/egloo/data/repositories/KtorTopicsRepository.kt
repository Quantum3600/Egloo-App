package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.TopicListResponse
import com.trishit.egloo.data.api.TopicResponse
import com.trishit.egloo.data.api.safeParse
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.domain.models.Topic
import io.ktor.client.*
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
            response.safeParse<TopicListResponse>().onSuccess { wrapped ->
                emit(wrapped.topics.map { it.toDomain() })
            }.onFailure {
                // Fallback for direct list if necessary
                val directResponse = client.get("/api/v1/topics")
                directResponse.safeParse<List<TopicResponse>>().onSuccess { list ->
                    emit(list.map { it.toDomain() })
                }.onFailure {
                    emit(emptyList())
                }
            }
        } catch (e: Exception) {
            println("KtorTopicsRepository: Error fetching topics: ${e.message}")
            emit(emptyList())
        }
    }

    override fun getTopicById(id: String): Flow<Topic?> = flow {
        try {
            val response = client.get("/api/v1/topics/$id")
            response.safeParse<TopicResponse>().onSuccess { dto ->
                emit(dto.toDomain())
            }.onFailure {
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
            else response.safeParse<Unit>().map { Unit }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun triggerTopicGeneration(): Result<Unit> {
        return try {
            val response = client.post("/api/v1/topics/refresh") {
                contentType(ContentType.Application.Json)
                setBody(com.trishit.egloo.data.api.RefreshTopicsRequest())
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else response.safeParse<Unit>().map { Unit }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
