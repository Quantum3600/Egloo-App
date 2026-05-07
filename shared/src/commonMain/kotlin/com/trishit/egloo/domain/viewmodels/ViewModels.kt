package com.trishit.egloo.domain.viewmodels

import com.trishit.egloo.data.repositories.*
import com.trishit.egloo.domain.models.*
import com.trishit.egloo.platform.DeepLinkHandler
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

    fun loadDigest() {
        scope.launch {
            digestRepo.getDailyDigest().collect { result ->
                _uiState.value = when (result) {
                    is DigestResult.Loading -> HomeUiState(isLoading = true)
                    is DigestResult.Success -> HomeUiState(isLoading = false, digest = result.digest)
                    is DigestResult.Error   -> HomeUiState(isLoading = false, error = result.message)
                }
            }
        }
    }
}

// ── Chat ──────────────────────────────────────────────────────────────────────

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isTyping: Boolean = false,
)

class ChatViewModel(private val chatRepo: ChatRepository) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            chatRepo.getChatHistory().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return
        _uiState.update { it.copy(isSending = true) }
        scope.launch {
            chatRepo.sendMessage(trimmedText)
            _uiState.update { it.copy(isSending = false) }
        }
    }

    fun clearChat() {
        scope.launch { chatRepo.clearHistory() }
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
                handleAuthResult(result)
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
        _uiState.update { it.copy(connectingSourceId = sourceId) }
        scope.launch {
            val sourceType = mapSourceIdToType(sourceId)
            if (sourceType != null) {
                connectedSourcesRepo.connectSource(sourceType)
            }
            _uiState.update { it.copy(connectingSourceId = null) }
        }
    }

    fun disconnectSource(id: String) {
        scope.launch { connectedSourcesRepo.disconnectSource(id) }
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
}

// ── Settings ──────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isSaved: Boolean = false,
)

class SettingsViewModel(private val settingsRepo: SettingsRepository) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            settingsRepo.getSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun updateUserName(name: String) = update { it.copy(userName = name) }
    fun toggleDarkTheme(enabled: Boolean) = update { it.copy(darkTheme = enabled) }
    fun togglePingoGreetings(enabled: Boolean) = update { it.copy(pingoGreetingsEnabled = enabled) }
    fun toggleDigestNotifications(enabled: Boolean) = update { it.copy(digestNotificationsEnabled = enabled) }
    fun setSyncFrequency(hours: Int) = update { it.copy(syncFrequencyHours = hours) }

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
