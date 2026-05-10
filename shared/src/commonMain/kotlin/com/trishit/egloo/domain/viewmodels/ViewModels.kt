package com.trishit.egloo.domain.viewmodels

import com.trishit.egloo.data.api.*
import com.trishit.egloo.data.repositories.*
import com.trishit.egloo.domain.models.*
import com.trishit.egloo.platform.DeepLinkHandler
import com.trishit.egloo.platform.currentTimeMillis
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ── Base ──────────────────────────────────────────────────────────────────────

abstract class BaseViewModel {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    fun dispose() = scope.cancel()
}

// ── Home / Digest ─────────────────────────────────────────────────────────────

data class HomeUiState(
    val isLoading: Boolean = true,
    val digest: DailyDigest? = null,
    val error: String? = null,
)

class HomeViewModel(private val digestRepo: DigestRepository) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadDigest() }

    fun loadDigest(force: Boolean = false) {
        scope.launch {
            if (force) {
                _uiState.update { it.copy(isLoading = true) }
                digestRepo.generateDigest(force = true)
            }
            digestRepo.getDailyDigest().collect { result ->
                _uiState.value = when (result) {
                    is DigestResult.Loading -> HomeUiState(isLoading = true)
                    is DigestResult.Success -> HomeUiState(isLoading = false, digest = result.digest)
                    is DigestResult.Error   -> HomeUiState(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun saveDigest(id: String) {
        scope.launch {
            digestRepo.saveDigest(id)
        }
    }
}

// ── Chat ──────────────────────────────────────────────────────────────────────

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isTyping: Boolean = false,
)

class ChatViewModel(
    private val chatRepo: ChatRepository,
    private val settingsRepo: SettingsRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var preferredModel: String? = null

    init {
        scope.launch {
            chatRepo.loadHistory()
        }
        scope.launch {
            chatRepo.getChatHistory().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        scope.launch {
            chatRepo.getSuggestions().collect { suggestions ->
                _uiState.update { it.copy(suggestions = suggestions) }
            }
        }
        scope.launch {
            settingsRepo.getSettings().collect { settings ->
                preferredModel = settings.preferredLlmModel
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return
        _uiState.update { it.copy(isSending = true, isTyping = true) }
        scope.launch {
            chatRepo.sendMessage(trimmedText, preferredModel)
            _uiState.update { it.copy(isSending = false, isTyping = false) }
        }
    }

    fun clearChat() {
        scope.launch { chatRepo.clearHistory() }
    }

    fun saveMessage(id: String) {
        scope.launch {
            chatRepo.saveMessage(id)
        }
    }
}

// ── Topics ────────────────────────────────────────────────────────────────────

data class TopicsUiState(
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val topics: List<Topic> = emptyList(),
    val selectedTopic: Topic? = null,
    val error: String? = null
)

class TopicsViewModel(private val topicsRepo: TopicsRepository) : BaseViewModel() {

    private val _uiState = MutableStateFlow(TopicsUiState())
    val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

    init {
        loadTopics()
    }

    fun loadTopics() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            topicsRepo.getTopics().collect { topics ->
                _uiState.update { it.copy(isLoading = false, topics = topics) }
            }
        }
    }

    fun selectTopic(topic: Topic?) {
        _uiState.update { it.copy(selectedTopic = topic) }
    }

    fun createTopic(name: String, summary: String) {
        scope.launch {
            topicsRepo.createTopic(name, summary).onSuccess {
                loadTopics()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun generateTopics() {
        scope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            topicsRepo.triggerTopicGeneration().onSuccess {
                // Poll for updates or just refresh after a delay in dummy mode
                delay(3000)
                loadTopics()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(isGenerating = false) }
        }
    }
}

// ── Sources ───────────────────────────────────────────────────────────────────

data class SourcesUiState(
    val availableSources: List<AvailableSource> = emptyList(),
    val connectedSources: List<ConnectedSource> = emptyList(),
    val sourceRows: List<SourceRowData> = emptyList(),
    val connectingSourceId: String? = null,
    val navigateToPdfUpload: Boolean = false,
    val authMessage: String? = null,
    val authMessageType: AuthMessageType? = null,
)

enum class AuthMessageType {
    SUCCESS, ERROR
}

class SourcesViewModel(
    private val availableSourcesRepo: AvailableSourcesRepository,
    private val connectedSourcesRepo: SourcesRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    init {
        // Load available sources (global)
        scope.launch {
            availableSourcesRepo.getAvailableSources().collect { available ->
                _uiState.update { it.copy(availableSources = available) }
                mergeSourceData()
            }
        }

        // Load connected sources (user-specific)
        scope.launch {
            connectedSourcesRepo.getConnectedSources().collect { connected ->
                _uiState.update { it.copy(connectedSources = connected) }
                mergeSourceData()
            }
        }

        // Listen for deep link results
        scope.launch {
            DeepLinkHandler.authResultFlow.collect { result ->
                result?.let {
                    handleAuthResult(it)
                    DeepLinkHandler.clearAuthResult()
                }
            }
        }
    }

    private fun mergeSourceData() {
        val state = _uiState.value
        val rows = state.availableSources.map { available ->
            val connected = state.connectedSources.find { 
                it.type.name.lowercase() == available.id.lowercase().replace("_", "")
                    || it.type.displayName.lowercase() == available.displayName.lowercase()
            }
            SourceRowData(
                availableSource = available,
                connectedSource = connected,
                isConnected = connected?.isConnected ?: false,
                itemCount = connected?.itemCount ?: 0,
                accountName = connected?.accountName ?: "",
                lastSyncedAt = connected?.lastSyncedAt,
                sourceId = available.id
            )
        }
        _uiState.update { it.copy(sourceRows = rows) }
    }

    fun connectSource(sourceId: String) {
        if (sourceId == "pdf_upload") {
            _uiState.update { it.copy(navigateToPdfUpload = true) }
            return
        }
        _uiState.update { it.copy(connectingSourceId = sourceId) }
        scope.launch {
            val sourceType = mapSourceIdToType(sourceId)
            if (sourceType != null) {
                connectedSourcesRepo.connectSource(sourceType)
            }
            _uiState.update { it.copy(connectingSourceId = null) }
        }
    }

    fun disconnectSource(sourceId: String) {
        scope.launch { connectedSourcesRepo.disconnectSource(sourceId) }
    }

    private fun mapSourceIdToType(sourceId: String): SourceType? {
        return when (sourceId) {
            "gmail" -> SourceType.GMAIL
            "slack" -> SourceType.SLACK
            "google_drive" -> SourceType.GOOGLE_DRIVE
            "notion" -> SourceType.NOTION
            "pdf" -> SourceType.PDF
            else -> null
        }
    }

    private fun handleAuthResult(result: DeepLinkHandler.AuthDeepLinkResult) {
        val messageType = if (result.status == "success") AuthMessageType.SUCCESS else AuthMessageType.ERROR
        val sourceName = when (result.source) {
            "gmail" -> "Gmail"
            "slack" -> "Slack"
            "google_drive" -> "Google Drive"
            "notion" -> "Notion"
            "pdf" -> "PDF"
            else -> result.source.replaceFirstChar { it.uppercase() }
        }
        val message = if (result.status == "success") {
            "Successfully connected $sourceName!"
        } else {
            "Failed to connect $sourceName. Please try again."
        }

        _uiState.update {
            it.copy(
                authMessage = message,
                authMessageType = messageType,
                connectingSourceId = null
            )
        }

        // Clear message after 3 seconds
        scope.launch {
            delay(3000)
            _uiState.update { it.copy(authMessage = null, authMessageType = null) }
        }

        // Refresh sources list on success
        if (result.status == "success") {
            scope.launch {
                delay(1000)
                connectedSourcesRepo.getConnectedSources().collect { sources ->
                    _uiState.update { it.copy(connectedSources = sources) }
                    mergeSourceData()
                }
            }
        }
    }

    fun clearAuthMessage() {
        _uiState.update { it.copy(authMessage = null, authMessageType = null) }
    }

    fun onPdfUploadNavigated() {
        _uiState.update { it.copy(navigateToPdfUpload = false) }
    }
}

// ── Settings ──────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val userProfile: UserResponse? = null,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val authRepo: AuthRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            settingsRepo.getSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        scope.launch {
            authRepo.getUserProfile().collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
    }

    fun logout() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepo.logout()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateUserName(name: String) = update { it.copy(userName = name) }
    fun toggleDarkTheme(enabled: Boolean) = update { it.copy(darkTheme = enabled) }
    fun togglePingoGreetings(enabled: Boolean) = update { it.copy(pingoGreetingsEnabled = enabled) }
    fun toggleDigestNotifications(enabled: Boolean) = update { it.copy(digestNotificationsEnabled = enabled) }
    fun setSyncFrequency(hours: Int) = update { it.copy(syncFrequencyHours = hours) }
    fun setPreferredLlmModel(model: String) = update { it.copy(preferredLlmModel = model) }

    private fun update(block: (AppSettings) -> AppSettings) {
        scope.launch {
            val updated = block(_uiState.value.settings)
            settingsRepo.updateSettings(updated)
            _uiState.update { it.copy(isSaved = true) }
            delay(1500)
            _uiState.update { it.copy(isSaved = false) }
        }
    }
}

// ── Saved ────────────────────────────────────────────────────────────────────

data class SavedUiState(
    val isLoading: Boolean = false,
    val items: List<SavedItem> = emptyList(),
    val error: String? = null
)

class SavedViewModel(private val repository: SavedRepository) : BaseViewModel() {
    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    init {
        loadSavedItems()
    }

    fun loadSavedItems() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getSavedItems().collect { items ->
                _uiState.update { it.copy(isLoading = false, items = items) }
            }
        }
    }

    fun unsaveItem(id: String) {
        scope.launch {
            repository.unsaveItem(id).onSuccess {
                loadSavedItems()
            }
        }
    }
}

// ── Brain / Proactive Intelligence ──────────────────────────────────────────

data class BrainUiState(
    val isLoading: Boolean = false,
    val today: BrainToday? = null,
    val missing: BrainMissing? = null,
    val connections: List<BrainConnection> = emptyList(),
    val alerts: List<BrainAlert> = emptyList(),
    val error: String? = null
)

class BrainViewModel(private val brainRepo: BrainRepository) : BaseViewModel() {
    private val _uiState = MutableStateFlow(BrainUiState())
    val uiState: StateFlow<BrainUiState> = _uiState.asStateFlow()

    init {
        loadBrainData()
    }

    fun loadBrainData() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Combine multiple flows for a unified state
            combine(
                brainRepo.getBrainToday(),
                brainRepo.getBrainMissing(),
                brainRepo.getBrainConnections(),
                brainRepo.getBrainAlerts()
            ) { today, missing, connections, alerts ->
                BrainUiState(
                    isLoading = false,
                    today = today,
                    missing = missing,
                    connections = connections,
                    alerts = alerts
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun dismissAlert(id: String) {
        // Logic to dismiss a single alert if backend supports it
        _uiState.update { it.copy(alerts = it.alerts.filter { a -> a.id != id }) }
    }

    fun clearAllAlerts() {
        scope.launch {
            brainRepo.clearAlerts().onSuccess {
                _uiState.update { it.copy(alerts = emptyList()) }
            }
        }
    }
}

// ── Ingest Job Tracking ───────────────────────────────────────────────────────

data class IngestUiState(
    val recentJobs: List<IngestJob> = emptyList(),
    val activeJobs: List<IngestJob> = emptyList(),
    val healthStatus: HealthStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class IngestViewModel(
    private val ingestRepo: IngestRepository,
    private val healthRepo: HealthRepository
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(IngestUiState())
    val uiState: StateFlow<IngestUiState> = _uiState.asStateFlow()

    // Track the last time health check was performed to prevent multiple simultaneous checks
    private var lastHealthCheckTime = 0L
    private val HEALTH_CHECK_INTERVAL = 300_000L  // 5 minutes in milliseconds

    init {
        loadJobs()
        monitorHealth()
    }

    private fun monitorHealth() {
        scope.launch {
            while (true) {
                val now = currentTimeMillis()
                // Only perform health check if at least 5 minutes have passed since the last one
                if (now - lastHealthCheckTime >= HEALTH_CHECK_INTERVAL) {
                    lastHealthCheckTime = now
                    try {
                        healthRepo.getHealthStatus().collect { status ->
                            _uiState.update { it.copy(healthStatus = status) }
                        }
                    } catch (e: Exception) {
                        // Log error but don't crash the monitoring loop
                        println("IngestViewModel: Health check failed: ${e.message}")
                    }
                }
                // Check every 30 seconds if we need to run health check
                delay(30_000)
            }
        }
    }

    fun loadJobs() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            ingestRepo.getRecentJobs().collect { jobs ->
                val active = jobs.filter { it.status in listOf("started", "queued", "processing") }
                _uiState.update { it.copy(recentJobs = jobs, activeJobs = active, isLoading = false) }
            }
        }
    }

    fun triggerSyncAll() {
        scope.launch {
            ingestRepo.triggerAllIngest().onSuccess { jobIds ->
                _uiState.update { it.copy(error = null) }
                loadJobs()
                // Poll job status until all complete
                pollJobsUntilComplete(jobIds)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun triggerSourceSync(sourceId: String) {
        scope.launch {
            ingestRepo.triggerIngest(sourceId).onSuccess { jobId ->
                _uiState.update { it.copy(error = null) }
                loadJobs()
                // Poll this job until complete
                pollJobsUntilComplete(listOf(jobId))
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private suspend fun pollJobsUntilComplete(jobIds: List<String>) {
        if (jobIds.isEmpty()) return
        
        val pendingJobIds = jobIds.toMutableSet()
        var pollCount = 0
        val maxPolls = 150  // 5 minutes at 2s interval
        
        while (pendingJobIds.isNotEmpty() && pollCount < maxPolls) {
            delay(2000)  // Poll every 2 seconds
            pollCount++
            
            val currentPending = pendingJobIds.toList()
            for (jobId in currentPending) {
                try {
                    ingestRepo.getJobStatus(jobId).firstOrNull()?.let { job ->
                        // Update the job in the list
                        _uiState.update { state ->
                            val updatedJobs = state.recentJobs.toMutableList()
                            val index = updatedJobs.indexOfFirst { it.id == jobId }
                            if (index != -1) {
                                updatedJobs[index] = job
                            } else if (updatedJobs.size < 50) { // Limit size
                                updatedJobs.add(0, job)
                            }
                            
                            val active = updatedJobs.filter { it.status in listOf("started", "queued", "processing") }
                            state.copy(recentJobs = updatedJobs, activeJobs = active)
                        }
                        
                        if (job.status !in listOf("started", "queued", "processing")) {
                            pendingJobIds.remove(jobId)
                        }
                    }
                } catch (_: Exception) {
                    // Continue polling on error
                }
            }
        }
    }
}

// ── Notifications / FCM ────────────────────────────────────────────────────────

data class NotificationUiState(
    val fcmToken: String? = null,
    val digestNotificationsEnabled: Boolean = true,
    val isRegistering: Boolean = false,
    val error: String? = null
)

class NotificationViewModel(private val notificationRepo: NotificationRepository) : BaseViewModel() {
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        // Listen for FCM token changes and check notification preferences
        scope.launch {
            notificationRepo.getFcmTokenStream().collect { token ->
                _uiState.update { it.copy(fcmToken = token) }
                if (token != null) {
                    registerToken(token)
                }
            }
        }

        // Load notification preferences
        scope.launch {
            val enabled = notificationRepo.isDigestNotificationEnabled()
            _uiState.update { it.copy(digestNotificationsEnabled = enabled) }
        }
    }

    fun registerToken(token: String) {
        scope.launch {
            _uiState.update { it.copy(isRegistering = true, error = null) }
            notificationRepo.registerToken(token).onSuccess {
                _uiState.update { it.copy(isRegistering = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isRegistering = false, error = e.message) }
            }
        }
    }

    fun toggleDigestNotifications(enabled: Boolean) {
        scope.launch {
            notificationRepo.setDigestNotificationEnabled(enabled).onSuccess {
                _uiState.update { it.copy(digestNotificationsEnabled = enabled) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
