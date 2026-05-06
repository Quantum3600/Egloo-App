package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.dummy.DummyData
import com.trishit.egloo.domain.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

// ─────────────────────────────────────────────────────────────────────────────
// Dummy implementations
// Replace with KtorXRepository when the backend is ready.
// See: data/api/ApiGuidelines.kt for the migration plan.
// ─────────────────────────────────────────────────────────────────────────────

class DummyDigestRepository : DigestRepository {
    override fun getDailyDigest(): Flow<DigestResult> = flow {
        emit(DigestResult.Loading)
        delay(900) // simulate network
        emit(DigestResult.Success(DummyData.dummyDigest))
    }
}

class DummyChatRepository : ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    override fun getChatHistory(): Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun sendMessage(text: String) {
        val now = kotlin.time.Clock.System.now()
        val userMsg = ChatMessage.User(
            id = "u_${now.toEpochMilliseconds()}",
            text = text,
            sentAt = now,
        )
        _messages.value = _messages.value + userMsg

        // Simulate Pingo thinking
        val thinkingMsg = ChatMessage.Pingo(
            id = "p_${now.toEpochMilliseconds() + 1}",
            text = "",
            isStreaming = true,
            sentAt = now,
        )
        _messages.value = _messages.value + thinkingMsg
        delay(1200)

        // Replace thinking with answer
        val answer = generateDummyAnswer(text)
        _messages.value = _messages.value.dropLast(1) + answer
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }

    private fun generateDummyAnswer(question: String): ChatMessage.Pingo {
        val lower = question.lowercase()
        val (text, sources) = when {
            "migration" in lower || "database" in lower ->
                Pair(
                    "The Postgres migration was postponed to Q3. Ali will lead the schema design sprint starting May 12. Documentation of the current schema needs to happen first.",
                    listOf(ChatSource("#project-alpha", SourceType.SLACK), ChatSource("Q2 Roadmap.gdoc", SourceType.GOOGLE_DRIVE))
                )
            "acme" in lower || "demo" in lower ->
                Pair(
                    "The Acme Corp demo is on May 9 at 2 PM. Rachel confirmed via email — they want to see the new onboarding flow.",
                    listOf(ChatSource("rachel@acmecorp.com", SourceType.GMAIL))
                )
            "budget" in lower || "infra" in lower ->
                Pair(
                    "\$24k was approved for the AWS infrastructure upgrade. The budget needs to be spent before June 30.",
                    listOf(ChatSource("finance@company.com", SourceType.GMAIL))
                )
            "roadmap" in lower || "q2" in lower ->
                Pair(
                    "Q2 features are locked: Search v2, Analytics dashboard, and Team invites. AI features were moved to Q3. The final doc is in Drive.",
                    listOf(ChatSource("Product Roadmap.gdoc", SourceType.GOOGLE_DRIVE))
                )
            else ->
                Pair(
                    "I found a few relevant items in your knowledge base. Let me search more specifically — try asking about Project Alpha, the Acme demo, or the infra budget.",
                    emptyList()
                )
        }
        return ChatMessage.Pingo(
            id = "p_${kotlin.time.Clock.System.now().toEpochMilliseconds()}",
            text = text,
            sources = sources,
            isStreaming = false,
            sentAt = kotlin.time.Clock.System.now(),
        )
    }
}

class DummyTopicsRepository : TopicsRepository {
    override fun getTopics(): Flow<List<Topic>> = flow {
        delay(400)
        emit(DummyData.dummyTopics)
    }

    override fun getTopicById(id: String): Flow<Topic?> = flow {
        emit(DummyData.dummyTopics.find { it.id == id })
    }
}

class DummySourcesRepository : SourcesRepository {
    private val _sources = MutableStateFlow(DummyData.connectedSources)

    override fun getConnectedSources(): Flow<List<ConnectedSource>> = _sources.asStateFlow()

    override suspend fun connectSource(type: SourceType) {
        delay(1500) // simulate OAuth
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds().toString()
        _sources.value = _sources.value.map { source ->
            if (source.type == type) source.copy(isConnected = true, lastSyncedAt = now, itemCount = 42)
            else source
        }
    }

    override suspend fun disconnectSource(id: String) {
        _sources.value = _sources.value.map { source ->
            if (source.id == id) source.copy(isConnected = false, lastSyncedAt = null, itemCount = 0)
            else source
        }
    }
}

class DummySettingsRepository : SettingsRepository {
    private val _settings = MutableStateFlow(DummyData.dummySettings)

    override fun getSettings(): Flow<AppSettings> = _settings.asStateFlow()

    override suspend fun updateSettings(settings: AppSettings) {
        _settings.value = settings
    }
}
