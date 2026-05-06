package com.trishit.egloo.data.repositories

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.trishit.egloo.domain.models.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepositoryImpl(private val settings: Settings) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = "app_settings"

    private val _settingsFlow = MutableStateFlow(loadSettings())

    override fun getSettings(): Flow<AppSettings> = _settingsFlow

    override suspend fun updateSettings(settings: AppSettings) {
        this.settings[key] = json.encodeToString(settings)
        _settingsFlow.value = settings
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
