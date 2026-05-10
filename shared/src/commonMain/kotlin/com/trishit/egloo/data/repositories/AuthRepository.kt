package com.trishit.egloo.data.repositories

import com.russhwolf.settings.Settings
import com.trishit.egloo.data.api.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

interface AuthRepository {
    val isAuthenticated: StateFlow<Boolean>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(email: String, password: String, name: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun getToken(): String?
    fun getRefreshToken(): String?
    suspend fun refreshToken(): Result<TokenResponse>
    fun getUserProfile(): Flow<UserResponse?>
}

class KtorAuthRepository(
    private val client: HttpClient,
    private val settings: Settings,
    private val baseUrl: String
) : AuthRepository {

    private val _isAuthenticated = MutableStateFlow(getToken() != null)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Separate client for auth operations to avoid circular dependency/interceptor issues
    private val authClient = HttpClient(client.engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    println("Ktor-Auth: $message")
                }
            }
        }
        defaultRequest {
            url(baseUrl)
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = authClient.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(UserLoginRequest(email, password))
            }
            if (response.status.isSuccess()) {
                val tokens = response.body<TokenResponse>()
                saveTokens(tokens)
                _isAuthenticated.value = true
                Result.success(Unit)
            } else {
                val errorMsg = try { response.bodyAsText() } catch (e: Exception) { response.status.description }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String, name: String): Result<Unit> {
        return try {
            val response = authClient.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(UserRegisterRequest(email, password, name))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Registration failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            client.post("/api/v1/auth/logout")
            clearTokens()
            _isAuthenticated.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            clearTokens()
            _isAuthenticated.value = false
            Result.failure(e)
        }
    }

    override fun getToken(): String? = settings.getStringOrNull("access_token")
    
    override fun getRefreshToken(): String? = settings.getStringOrNull("refresh_token")

    override suspend fun refreshToken(): Result<TokenResponse> {
        val refreshToken = getRefreshToken() ?: return Result.failure(Exception("No refresh token available"))
        
        return try {
            val response = authClient.post("/api/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken))
            }
            
            if (response.status.isSuccess()) {
                val tokens = response.body<TokenResponse>()
                saveTokens(tokens)
                Result.success(tokens)
            } else {
                clearTokens()
                _isAuthenticated.value = false
                Result.failure(Exception("Refresh failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveTokens(tokens: TokenResponse) {
        settings.putString("access_token", tokens.access_token)
        settings.putString("refresh_token", tokens.refresh_token)
    }

    private fun clearTokens() {
        settings.remove("access_token")
        settings.remove("refresh_token")
    }

    override fun getUserProfile(): Flow<UserResponse?> = flow {
        try {
            val response = client.get("/api/v1/auth/me")
            if (response.status.isSuccess()) {
                emit(response.body<UserResponse>())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
}
