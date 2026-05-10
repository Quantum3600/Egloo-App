package com.trishit.egloo.data.api

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createHttpClient(
    baseUrl: String,
    tokenProvider: () -> String?,
    refreshTokenProvider: suspend () -> Result<TokenResponse>
) = HttpClient(platformEngine()) {
    defaultRequest {
        url(baseUrl)
        header("ngrok-skip-browser-warning", "true")
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        })
    }

    install(Auth) {
        bearer {
            loadTokens {
                tokenProvider()?.let { BearerTokens(it, "") }
            }
            refreshTokens {
                val result = refreshTokenProvider()
                result.getOrNull()?.let {
                    BearerTokens(it.access_token, it.refresh_token)
                }
            }
            // Send token for all requests to the base URL
            sendWithoutRequest { request ->
                request.url.toString().startsWith(baseUrl)
            }
        }
    }

    install(Logging) {
        level = LogLevel.INFO // Reset to INFO since we are not using streams anymore
        logger = object : Logger {
            override fun log(message: String) {
                println("Ktor: $message")
            }
        }
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 300_000 // Increased to 5 minutes for long-running AI streams
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 300_000
    }
}

expect fun platformEngine(): HttpClientEngineFactory<*>
