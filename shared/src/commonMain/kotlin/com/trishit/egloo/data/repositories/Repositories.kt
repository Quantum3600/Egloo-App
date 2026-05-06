package com.trishit.egloo.data.repositories

import com.trishit.egloo.domain.models.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// Repository interfaces
//
// These interfaces are the boundary between UI/ViewModels and data sources.
// Today: DummyXRepository provides fake data.
// Later: KtorXRepository talks to the real backend.
// Swap the binding in the Koin module — no screen code changes.
// ─────────────────────────────────────────────────────────────────────────────

interface DigestRepository {
    /** Emits the digest for today. Will emit a loading state then data. */
    fun getDailyDigest(): Flow<DigestResult>
}

interface ChatRepository {
    /** Returns existing chat history as a flow. */
    fun getChatHistory(): Flow<List<ChatMessage>>
    /** Sends a user message. Pingo's reply will be emitted into getChatHistory(). */
    suspend fun sendMessage(text: String)
    /** Clears the conversation. */
    suspend fun clearHistory()
}

interface TopicsRepository {
    fun getTopics(): Flow<List<Topic>>
    fun getTopicById(id: String): Flow<Topic?>
}

interface SourcesRepository {
    fun getConnectedSources(): Flow<List<ConnectedSource>>
    /** Triggers mock OAuth flow. Real implementation opens a browser/WebView. */
    suspend fun connectSource(type: SourceType)
    suspend fun disconnectSource(id: String)
}

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
}

// ── Result wrappers ───────────────────────────────────────────────────────────

sealed class DigestResult {
    data object Loading : DigestResult()
    data class Success(val digest: DailyDigest) : DigestResult()
    data class Error(val message: String) : DigestResult()
}
