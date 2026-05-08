package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.AvailableSourceDto
import com.trishit.egloo.data.api.AvailableSourceListResponse
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.domain.models.AvailableSource
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorAvailableSourcesRepository(private val client: HttpClient) : AvailableSourcesRepository {
    override fun getAvailableSources(): Flow<List<AvailableSource>> = flow {
        try {
            val response = client.get("/api/v1/sources/available")
            if (response.status.value == 200) {
                // API returns list directly or wrapped in AvailableSourceListResponse
                val sources = try {
                    response.body<List<AvailableSourceDto>>()
                } catch (e: Exception) {
                    response.body<AvailableSourceListResponse>().sources
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
}

