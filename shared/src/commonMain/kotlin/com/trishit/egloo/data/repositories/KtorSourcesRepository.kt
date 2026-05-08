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
            SourceType.GOOGLE_DRIVE, SourceType.DRIVE -> "gmail" // Mapping Drive to Gmail flow as per API summary
            else -> type.name.lowercase()
        }
        try {
            val response = client.get("/api/v1/sources/connect/$typeStr")
            if (response.status.value == 200 || response.status.value == 307 || response.status.value == 308) {
                // Server may return redirect or JSON with oauthUrl
                val oauthUrl = try {
                    val body = response.body<Map<String, String>>()
                    body["oauthUrl"]
                } catch (_: Exception) {
                    response.headers["Location"] // Redirect URL in header
                }
                oauthUrl?.let { platformOpenUrl(it) }
            }
        } catch (e: Exception) {
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
