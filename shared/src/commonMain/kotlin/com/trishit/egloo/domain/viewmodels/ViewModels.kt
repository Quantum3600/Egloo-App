package com.trishit.egloo.domain.viewmodels

import com.trishit.egloo.data.repositories.*
import com.trishit.egloo.domain.models.*
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
    val topics: List<Topic> = emptyList(),
    val selectedTopic: Topic? = null,
)

class TopicsViewModel(private val topicsRepo: TopicsRepository) : BaseViewModel() {

    private val _uiState = MutableStateFlow(TopicsUiState())
    val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            topicsRepo.getTopics().collect { topics ->
                _uiState.update { it.copy(isLoading = false, topics = topics) }
            }
        }
    }

    fun selectTopic(topic: Topic?) {
        _uiState.update { it.copy(selectedTopic = topic) }
    }
}

// ── Sources ───────────────────────────────────────────────────────────────────

data class SourcesUiState(
    val sources: List<ConnectedSource> = emptyList(),
    val connectingType: SourceType? = null,
)

class SourcesViewModel(private val sourcesRepo: SourcesRepository) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            sourcesRepo.getConnectedSources().collect { sources ->
                _uiState.update { it.copy(sources = sources) }
            }
        }
    }

    fun connectSource(type: SourceType) {
        _uiState.update { it.copy(connectingType = type) }
        scope.launch {
            sourcesRepo.connectSource(type)
            _uiState.update { it.copy(connectingType = null) }
        }
    }

    fun disconnectSource(id: String) {
        scope.launch { sourcesRepo.disconnectSource(id) }
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
