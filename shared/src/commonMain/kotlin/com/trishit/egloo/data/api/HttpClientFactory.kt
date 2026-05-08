package com.trishit.egloo.data.api

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createHttpClient(
    baseUrl: String,
    tokenProvider: () -> String?,
    refreshTokenProvider: suspend () -> Result<TokenResponse>
) = HttpClient(platformEngine()) {
    defaultRequest {
        url(baseUrl)
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
        level = LogLevel.INFO // Adjust to BODY for deep debugging
        logger = Logger.DEFAULT
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
    }
}

expect fun platformEngine(): HttpClientEngineFactory<*>
