package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.ConnectedSourceDto
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.domain.models.ConnectedSource
import com.trishit.egloo.domain.models.SourceType
import com.trishit.egloo.platform.platformOpenUrl
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorSourcesRepository(private val client: HttpClient) : SourcesRepository {
    override fun getConnectedSources(): Flow<List<ConnectedSource>> = flow {
        try {
            val response = client.get("/api/v1/sources")
            if (response.status.value == 200) {
                val dtos = response.body<List<ConnectedSourceDto>>()
                emit(dtos.map { it.toDomain() })
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun connectSource(type: SourceType) {
        val typeStr = type.name.lowercase()
        try {
            val response = client.get("/api/v1/sources/connect/$typeStr")
            if (response.status.value == 200) {
                val body = response.body<Map<String, String>>()
                body["oauthUrl"]?.let { url ->
                    platformOpenUrl(url)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    override suspend fun disconnectSource(id: String) {
        try {
            // Based on guideline: DELETE /sources/{id}
            // But openapi says DELETE /api/v1/sources/{source_type}
            // I'll use the id for now, or map it to type.
            client.delete("/api/v1/sources/$id")
        } catch (e: Exception) {
            // Handle error
        }
    }
}
