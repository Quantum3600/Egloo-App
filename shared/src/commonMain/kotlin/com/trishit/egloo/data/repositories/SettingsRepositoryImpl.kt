package com.trishit.egloo.data.repositories

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.trishit.egloo.data.api.AppSettingsDto
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.data.api.toDto
import com.trishit.egloo.domain.models.AppSettings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepositoryImpl(
    private val settings: Settings,
    private val client: HttpClient
) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = "app_settings"

    private val _settingsFlow = MutableStateFlow(loadSettings())

    override fun getSettings(): Flow<AppSettings> = _settingsFlow

    override suspend fun updateSettings(settings: AppSettings) {
        // Save locally first for responsiveness
        this.settings[key] = json.encodeToString(settings)
        _settingsFlow.value = settings

        // Sync with backend
        try {
            client.put("/api/v1/settings") {
                contentType(ContentType.Application.Json)
                setBody(settings.toDto())
            }
        } catch (e: Exception) {
            // Log sync failure, could implement retry
        }
    }

    suspend fun fetchRemoteSettings() {
        try {
            val response = client.get("/api/v1/settings")
            if (response.status.isSuccess()) {
                val remoteSettings = response.body<AppSettingsDto>().toDomain()
                this.settings[key] = json.encodeToString(remoteSettings)
                _settingsFlow.value = remoteSettings
            }
        } catch (e: Exception) {
            // Fallback to local
        }
    }

    private fun loadSettings(): AppSettings {
        val saved = settings.getStringOrNull(key)
        return if (saved != null) {
            try {
                json.decodeFromString<AppSettings>(saved)
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }
}
