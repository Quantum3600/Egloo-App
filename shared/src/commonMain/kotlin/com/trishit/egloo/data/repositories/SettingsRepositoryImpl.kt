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
    private val settings: Settings
) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = "app_settings"

    private val _settingsFlow = MutableStateFlow(loadSettings())

    override fun getSettings(): Flow<AppSettings> = _settingsFlow

    override suspend fun updateSettings(settings: AppSettings) {
        // Save locally only as per backend constraints
        this.settings[key] = json.encodeToString(settings)
        _settingsFlow.value = settings
    }

    suspend fun fetchRemoteSettings() {
        // No-op: Settings are local-only as per backend contract
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
