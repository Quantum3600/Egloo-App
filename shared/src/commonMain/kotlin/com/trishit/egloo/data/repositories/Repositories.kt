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
    /** Manually triggers a new digest generation. */
    suspend fun generateDigest(force: Boolean = false): Result<Unit>
    /** Saves a digest as a bookmark. */
    suspend fun saveDigest(id: String): Result<Unit>
}

interface ChatRepository {
    /** Returns existing chat history as a flow. */
    fun getChatHistory(): Flow<List<ChatMessage>>
    /** Loads history from backend. */
    suspend fun loadHistory()
    /** Sends a user message. Pingo's reply will be emitted into getChatHistory(). */
    suspend fun sendMessage(text: String)
    /** Clears the conversation. */
    suspend fun clearHistory()
    /** Saves a chat result as a bookmark. */
    suspend fun saveMessage(id: String): Result<Unit>
}

interface TopicsRepository {
    fun getTopics(): Flow<List<Topic>>
    fun getTopicById(id: String): Flow<Topic?>
    suspend fun createTopic(name: String, summary: String): Result<Unit>
    suspend fun triggerTopicGeneration(): Result<Unit>
}

interface SourcesRepository {
    fun getConnectedSources(): Flow<List<ConnectedSource>>
    /** Triggers mock OAuth flow. Real implementation opens a browser/WebView. */
    suspend fun connectSource(type: SourceType)
    suspend fun disconnectSource(id: String)
}

interface AvailableSourcesRepository {
    fun getAvailableSources(): Flow<List<AvailableSource>>
}

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
}

interface SavedRepository {
    fun getSavedItems(): Flow<List<SavedItem>>
    suspend fun saveItem(id: String, type: String): Result<Unit>
    suspend fun unsaveItem(id: String): Result<Unit>
}

interface PdfRepository {
    fun getUploadedPdfs(): Flow<List<UploadedPdf>>
    suspend fun uploadPdf(filename: String, fileBytes: ByteArray): Result<UploadedPdf>
    suspend fun deletePdf(pdfId: String): Result<Unit>
    suspend fun reindexPdf(pdfId: String): Result<Unit>
}

interface BrainRepository {
    fun getBrainToday(): Flow<BrainToday>
    fun getBrainMissing(): Flow<BrainMissing>
    fun getBrainConnections(): Flow<List<BrainConnection>>
    fun getBrainAlerts(): Flow<List<BrainAlert>>
    suspend fun clearAlerts(): Result<Unit>
}

interface IngestRepository {
    fun getRecentJobs(): Flow<List<IngestJob>>
    fun getJobStatus(jobId: String): Flow<IngestJob>
    suspend fun triggerIngest(sourceId: String): Result<String> // Returns jobId
    suspend fun triggerAllIngest(): Result<List<String>> // Returns jobIds
}

// ── Result wrappers ───────────────────────────────────────────────────────────

sealed class DigestResult {
    data object Loading : DigestResult()
    data class Success(val digest: DailyDigest) : DigestResult()
    data class Error(val message: String) : DigestResult()
}
