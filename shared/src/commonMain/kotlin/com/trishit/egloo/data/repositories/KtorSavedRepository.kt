package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.*
import com.trishit.egloo.domain.models.SavedItem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorSavedRepository(private val client: HttpClient) : SavedRepository {
    override fun getSavedItems(): Flow<List<SavedItem>> = flow {
        try {
            val response = client.get("/api/v1/saved")
            if (response.status.isSuccess()) {
                val items = response.body<List<SavedItemResponse>>()
                emit(items.map { it.toDomain() })
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun saveItem(id: String, type: String): Result<Unit> {
        return try {
            val response = client.post("/api/v1/saved") {
                contentType(ContentType.Application.Json)
                setBody(SaveItemRequest(item_id = id, item_type = type))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save item: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unsaveItem(id: String): Result<Unit> {
        return try {
            val response = client.delete("/api/v1/saved/$id")
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unsave item: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
