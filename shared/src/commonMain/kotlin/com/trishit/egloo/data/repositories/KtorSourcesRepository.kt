package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.SourceListResponse
import com.trishit.egloo.data.api.SourceResponse
import com.trishit.egloo.data.api.safeParse
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.domain.models.ConnectedSource
import com.trishit.egloo.domain.models.SourceType
import com.trishit.egloo.platform.platformOpenUrl
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorSourcesRepository(private val client: HttpClient) : SourcesRepository {
    override fun getConnectedSources(): Flow<List<ConnectedSource>> = flow {
        try {
            val response = client.get("/api/v1/sources")
            response.safeParse<List<SourceResponse>>().onSuccess { sources ->
                emit(sources.map { it.toDomain() })
            }.onFailure {
                // Fallback for wrapped response if necessary
                val wrappedResponse = client.get("/api/v1/sources")
                wrappedResponse.safeParse<SourceListResponse>().onSuccess { wrapped ->
                    emit(wrapped.sources.map { it.toDomain() })
                }.onFailure {
                    emit(emptyList())
                }
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
            // Use client.config to disable redirects for this specific request.
            // This allows us to catch the 302/307/308 from the backend
            // and open the browser with the Location URL.
            val response = client.config {
                followRedirects = false
            }.get("/api/v1/sources/connect/$typeStr")
            
            val oauthUrl = when (response.status.value) {
                200 -> {
                    // Backend returns JSON { "oauthUrl": "..." }
                    response.safeParse<Map<String, String>>().getOrNull()?.get("oauthUrl")
                }
                301, 302, 303, 307, 308 -> {
                    // Backend returns a redirect to the provider
                    response.headers["Location"]
                }
                else -> {
                    println("Server returned unexpected status ${response.status} for $typeStr")
                    null
                }
            }

            if (oauthUrl != null) {
                println("Redirecting to OAuth URL: $oauthUrl")
                platformOpenUrl(oauthUrl)
            } else {
                println("Error: No OAuth URL found in response for $typeStr (Status: ${response.status})")
                // If it was a 200 OK but HTML, it might be because Ktor followed redirect 
                // despite client.config (unlikely in Ktor 3.x if configured correctly).
                if (response.status == HttpStatusCode.OK) {
                    val contentType = response.headers[HttpHeaders.ContentType]
                    println("Response was 200 OK but Content-Type was $contentType")
                }
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
