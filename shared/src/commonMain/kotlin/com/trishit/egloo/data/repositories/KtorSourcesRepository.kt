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
                // API returns list directly or wrapped in SourceListResponse
                val sources = try {
                    response.body<List<SourceResponse>>()
                } catch (_: Exception) {
                    response.body<SourceListResponse>().sources
                }
                emit(sources.map { it.toDomain() })
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override suspend fun connectSource(type: SourceType) {
        val typeStr = when (type) {
            SourceType.GOOGLE_DRIVE, SourceType.DRIVE -> "google_drive"
            SourceType.GMAIL -> "gmail"
            SourceType.SLACK -> "slack"
            SourceType.NOTION -> "notion"
            else -> type.name.lowercase()
        }
        
        try {
            // Use a separate request to avoid interceptor if needed, 
            // but usually we want to be authenticated to connect a source.
            val response = client.get("/api/v1/sources/connect/$typeStr")
            
            val oauthUrl = when (response.status.value) {
                200 -> {
                    val body = response.body<Map<String, String>>()
                    body["oauthUrl"]
                }
                301, 302, 303, 307, 308 -> {
                    response.headers["Location"]
                }
                else -> null
            }

            if (oauthUrl != null) {
                platformOpenUrl(oauthUrl)
            } else {
                println("Error: No OAuth URL found in response for $typeStr (Status: ${response.status})")
            }
        } catch (e: Exception) {
            println("Exception connecting to source $typeStr: ${e.message}")
            e.printStackTrace()
        }
    }

    override suspend fun disconnectSource(id: String) {
        try {
            // API expects DELETE /api/v1/sources/{source_type}
            // 'id' here is the source type string or the actual ID from ConnectedSource
            client.delete("/api/v1/sources/$id")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
