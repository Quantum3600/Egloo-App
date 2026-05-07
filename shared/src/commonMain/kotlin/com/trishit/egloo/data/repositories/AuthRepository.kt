package com.trishit.egloo.data.repositories

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.trishit.egloo.data.api.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.*

interface AuthRepository {
    val isAuthenticated: StateFlow<Boolean>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(email: String, password: String, name: String): Result<Unit>
    fun logout()
    fun getToken(): String?
}

class KtorAuthRepository(
    private val client: HttpClient,
    private val settings: Settings
) : AuthRepository {

    private val _isAuthenticated = MutableStateFlow(getToken() != null)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = client.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(UserLoginRequest(email, password))
            }
            if (response.status.isSuccess()) {
                val tokens = response.body<TokenResponse>()
                settings.putString("access_token", tokens.access_token)
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
            val response = client.post("/api/v1/auth/register") {
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

    override fun logout() {
        settings.remove("access_token")
        _isAuthenticated.value = false
    }

    override fun getToken(): String? {
        return settings.getStringOrNull("access_token")
    }
}
