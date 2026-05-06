package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.TopicResponse
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.domain.models.Topic
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorTopicsRepository(private val client: HttpClient) : TopicsRepository {
    override fun getTopics(): Flow<List<Topic>> = flow {
        try {
            val response = client.get("/api/v1/topics")
            if (response.status.value == 200) {
                val dtos = response.body<List<TopicResponse>>()
                emit(dtos.map { it.toDomain() })
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
}
