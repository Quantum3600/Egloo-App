package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.AvailableSourceDto
import com.trishit.egloo.data.api.AvailableSourceListResponse
import com.trishit.egloo.data.api.safeParse
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.domain.models.AvailableSource
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorAvailableSourcesRepository(private val client: HttpClient) : AvailableSourcesRepository {
    override fun getAvailableSources(): Flow<List<AvailableSource>> = flow {
        try {
            val response = client.get("/api/v1/sources/available")
            response.safeParse<List<AvailableSourceDto>>().onSuccess { sources ->
                emit(sources.map { it.toDomain() })
            }.onFailure {
                // Fallback for wrapped response if necessary
                val wrappedResponse = client.get("/api/v1/sources/available")
                wrappedResponse.safeParse<AvailableSourceListResponse>().onSuccess { wrapped ->
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
}

