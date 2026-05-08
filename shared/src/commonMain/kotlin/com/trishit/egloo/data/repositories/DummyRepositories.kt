package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.dummy.DummyData
import com.trishit.egloo.domain.models.*
import com.trishit.egloo.platform.currentTimeMillis
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

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
        val now = currentTimeMillis()
        val userMsg = ChatMessage.User(
            id = "u_$now",
            text = text,
            sentAt = now,
        )
        _messages.value = _messages.value + userMsg

        // Simulate Pingo thinking
        val thinkingMsg = ChatMessage.Pingo(
            id = "p_${now + 1}",
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
            id = "p_${currentTimeMillis()}",
            text = text,
            sources = sources,
            isStreaming = false,
            sentAt = currentTimeMillis(),
        )
    }
}

class DummyTopicsRepository : TopicsRepository {
    private val _topics = MutableStateFlow(DummyData.dummyTopics)

    override fun getTopics(): Flow<List<Topic>> = _topics.asStateFlow()

    override fun getTopicById(id: String): Flow<Topic?> = flow {
        emit(_topics.value.find { it.id == id })
    }

    override suspend fun createTopic(name: String, summary: String): Result<Unit> {
        delay(800)
        val newTopic = Topic(
            id = "t_${currentTimeMillis()}",
            title = name,
            summary = summary,
            itemCount = 0,
            sources = emptyList(),
            lastUpdatedAt = "Just now",
            color = TopicColor.CORAL
        )
        _topics.value = listOf(newTopic) + _topics.value
        return Result.success(Unit)
    }

    override suspend fun triggerTopicGeneration(): Result<Unit> {
        delay(2000)
        return Result.success(Unit)
    }
}

class DummySourcesRepository : SourcesRepository {
    private val _sources = MutableStateFlow(DummyData.connectedSources)

    override fun getConnectedSources(): Flow<List<ConnectedSource>> = _sources.asStateFlow()

    override suspend fun connectSource(type: SourceType) {
        delay(1500) // simulate OAuth
        val now = currentTimeMillis().toString()
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

class DummySavedRepository : SavedRepository {
    private val _saved = MutableStateFlow<List<SavedItem>>(emptyList())
    override fun getSavedItems(): Flow<List<SavedItem>> = _saved.asStateFlow()
    override suspend fun saveItem(id: String, type: String): Result<Unit> {
        val item = SavedItem(id, "Saved $type", "Summary of $type", type, "Now")
        _saved.value = _saved.value + item
        return Result.success(Unit)
    }
    override suspend fun unsaveItem(id: String): Result<Unit> {
        _saved.value = _saved.value.filter { it.id != id }
        return Result.success(Unit)
    }
}

class DummyAvailableSourcesRepository : AvailableSourcesRepository {
    override fun getAvailableSources(): Flow<List<AvailableSource>> = flow {
        emit(DummyData.availableSources)
    }
}

class DummyPdfRepository : PdfRepository {
    private val _pdfs = MutableStateFlow<List<UploadedPdf>>(emptyList())
    override fun getUploadedPdfs(): Flow<List<UploadedPdf>> = _pdfs.asStateFlow()
    override suspend fun uploadPdf(filename: String, fileBytes: ByteArray): Result<UploadedPdf> {
        delay(1000)
        val pdf = UploadedPdf(
            id = "pdf_${currentTimeMillis()}",
            filename = filename,
            pages = (1..50).random(),
            status = "indexed",
            uploadedAt = "Just now",
            fileSize = fileBytes.size.toLong()
        )
        _pdfs.value = _pdfs.value + pdf
        return Result.success(pdf)
    }
    override suspend fun deletePdf(pdfId: String): Result<Unit> {
        _pdfs.value = _pdfs.value.filter { it.id != pdfId }
        return Result.success(Unit)
    }
    override suspend fun reindexPdf(pdfId: String): Result<Unit> = Result.success(Unit)
}

class DummyBrainRepository : BrainRepository {
    override fun getBrainToday(): Flow<BrainToday> = flow {
        delay(800)
        emit(DummyData.dummyBrainToday)
    }

    override fun getBrainMissing(): Flow<BrainMissing> = flow {
        delay(1000)
        emit(DummyData.dummyBrainMissing)
    }

    override fun getBrainConnections(): Flow<List<BrainConnection>> = flow {
        delay(1200)
        emit(DummyData.dummyBrainConnections)
    }

    override fun getBrainAlerts(): Flow<List<BrainAlert>> = flow {
        delay(500)
        emit(DummyData.dummyBrainAlerts)
    }

    override suspend fun clearAlerts(): Result<Unit> = Result.success(Unit)
}

class DummyIngestRepository : IngestRepository {
    private val _jobs = MutableStateFlow(DummyData.dummyIngestJobs)

    override fun getRecentJobs(): Flow<List<IngestJob>> = _jobs

    override fun getJobStatus(jobId: String): Flow<IngestJob> = _jobs.map { jobs ->
        jobs.find { it.id == jobId } ?: jobs.first()
    }

    override suspend fun triggerIngest(sourceId: String): Result<String> {
        val jobId = "job_${currentTimeMillis()}"
        val newJob = IngestJob(
            id = jobId,
            sourceId = sourceId,
            sourceType = "MANUAL",
            status = "started",
            progress = 0,
            message = "Ingestion started...",
            createdAt = "Now",
            updatedAt = "Now"
        )
        _jobs.update { it + newJob }
        
        // Simulate progress
        CoroutineScope(Dispatchers.Default).launch {
            for (p in 10..100 step 20) {
                delay(1000)
                _jobs.update { jobs ->
                    jobs.map { 
                        if (it.id == jobId) it.copy(
                            progress = p, 
                            status = if (p == 100) "success" else "started",
                            updatedAt = "Now"
                        ) else it
                    }
                }
            }
        }
        
        return Result.success(jobId)
    }

    override suspend fun triggerAllIngest(): Result<List<String>> {
        val jobIds = listOf("job_all_1", "job_all_2")
        jobIds.forEach { triggerIngest(it) }
        return Result.success(jobIds)
    }
}
