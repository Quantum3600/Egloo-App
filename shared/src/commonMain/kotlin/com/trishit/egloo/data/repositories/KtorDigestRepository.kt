package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.DigestResponse
import com.trishit.egloo.data.api.toDomain
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorDigestRepository(private val client: HttpClient) : DigestRepository {
    override fun getDailyDigest(): Flow<DigestResult> = flow {
        emit(DigestResult.Loading)
        try {
            val response = client.get("/api/v1/digest/today")
            if (response.status.value == 200) {
                val dto = response.body<DigestResponse>()
                emit(DigestResult.Success(dto.toDomain()))
            } else {
                emit(DigestResult.Error("Failed to load digest: ${response.status}"))
            }
        } catch (e: Exception) {
            emit(DigestResult.Error(e.message ?: "Unknown error"))
        }
    }
}
