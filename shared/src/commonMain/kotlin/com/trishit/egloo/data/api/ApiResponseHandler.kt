package com.trishit.egloo.data.api

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackendError(val detail: String)

/**
 * Shared JSON configuration for the entire app.
 */
val sharedJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = true
}

/**
 * Safely parses an HttpResponse into a Result<T>.
 * Offloads parsing to Dispatchers.Default to avoid blocking the main thread.
 * Handles backend error payloads properly.
 */
suspend inline fun <reified T> HttpResponse.safeParse(): Result<T> {
    return withContext(Dispatchers.Default) {
        val rawBody = try {
            bodyAsText()
        } catch (e: Exception) {
            println("Ktor-Response: Failed to read body: ${e.message}")
            ""
        }
        
        println("Ktor-Response [${status}] ${call.request.url}: $rawBody")

        if (status.isSuccess()) {
            try {
                // If T is Unit, we don't need to parse the body
                if (T::class == Unit::class) {
                    return@withContext Result.success(Unit as T)
                }
                
                val parsed = sharedJson.decodeFromString<T>(rawBody)
                Result.success(parsed)
            } catch (e: Exception) {
                println("Ktor-Parse-Success-Error: ${e.message} | Path: ${call.request.url}")
                Result.failure(Exception("Failed to parse success body: ${e.message}"))
            }
        } else {
            try {
                val error = sharedJson.decodeFromString<BackendError>(rawBody)
                println("Ktor-Backend-Error: ${error.detail}")
                Result.failure(Exception(error.detail))
            } catch (e: Exception) {
                // Fallback for simple Map or unknown error format
                try {
                    val map = sharedJson.decodeFromString<Map<String, String>>(rawBody)
                    Result.failure(Exception(map["detail"] ?: "Server returned ${status}"))
                } catch (_: Exception) {
                    Result.failure(Exception("Server returned ${status}: $rawBody"))
                }
            }
        }
    }
}
