package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.SourceListResponse
import com.trishit.egloo.data.api.SourceResponse
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
                val listResponse = response.body<SourceListResponse>()
                emit(listResponse.sources.map { it.toDomain() })
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
            // Based on API_DOCS.json: DELETE /api/v1/sources/{type}
            // The 'id' passed here is expected to be the source type string or we should map it.
            // For now, assuming 'id' is the type or can be used as such.
            client.delete("/api/v1/sources/$id")
        } catch (e: Exception) {
            // Handle error
        }
    }
}
