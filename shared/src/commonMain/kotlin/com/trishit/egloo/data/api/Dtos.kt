package com.trishit.egloo.data.api

import com.trishit.egloo.domain.models.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.*
import kotlin.time.Clock

// ── Common Response Wrapper ──────────────────────────────────────────────────

@Serializable
data class EglooResponse<T>(
    val data: T? = null,
    val result: T? = null,
    val content: T? = null,
    val status: String? = null,
    val error: String? = null
) {
    fun getOrNull(): T? = data ?: result ?: content
    
    fun getOrThrow(): T {
        return getOrNull() ?: throw Exception(error ?: "Empty response")
    }
}

@Serializable
data class UserRegisterRequest(
    val email: String,
    val password: String,
    val full_name: String? = null
)

@Serializable
data class UserLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "bearer"
)

@Serializable
data class RefreshTokenRequest(
    val refresh_token: String
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val full_name: String?,
    val is_active: Boolean,
    val created_at: String
)

// ── Notification DTOs ────────────────────────────────────────────────────────

@Serializable
data class NotificationRegisterRequest(
    val fcm_token: String,
    val device_type: String = "android" // Default, will be updated by platform
)

@Serializable
data class NotificationPreferenceRequest(
    val digest_enabled: Boolean
)

// ── Digest DTOs ──────────────────────────────────────────────────────────────

@Serializable
data class DigestResponse(
    val id: String? = null,
    val date: String? = null,
    val date_label: String? = null,
    val greeting: String? = null,
    val pingo_message: String? = null,
    val summary_text: String? = null,
    val total_item_count: Int? = null,
    val sections: List<DigestSectionDto>? = emptyList(),
    val action_items: List<String>? = emptyList(),
    val topics: List<TopicResponse>? = emptyList(),
    val created_at: String? = null,
    val model_used: String? = null,
    val provider: String? = null,
    val usage: AIUsageDto? = null,
    val latency_ms: Long? = null
)

@Serializable
data class GenerateDigestRequest(
    @SerialName("force_regenerate") val force_regenerate: Boolean = false,
    @SerialName("fcm_token") val fcm_token: String? = null,
    @SerialName("target_date") val target_date: String? = null
)

@Serializable
data class DigestSectionDto(
    val title: String,
    val subtitle: String,
    val items: List<KnowledgeItemDto>,
    val action_items: List<ActionItemDto> = emptyList()
)

@Serializable
data class KnowledgeItemDto(
    val id: String,
    val title: String,
    val summary: String,
    val source_type: String,
    val source_name: String,
    val timestamp: String,
    val tags: List<String> = emptyList()
)

@Serializable
data class ActionItemDto(
    val id: String,
    val text: String,
    val source_type: String,
    val is_completed: Boolean = false
)

// ── Topics DTOs ──────────────────────────────────────────────────────────────

@Serializable
data class TopicResponse(
    val id: String? = null,
    val name: String? = null,
    val summary: String? = "",
    @SerialName("source_types") val source_types: List<String>? = emptyList(),
    @SerialName("item_count") val item_count: Int = 0,
    @SerialName("last_refreshed_at") val last_refreshed_at: String? = null,
    @SerialName("created_at") val created_at: String? = null
)

@Serializable
data class TopicListResponse(
    val topics: List<TopicResponse>,
    val total: Int
)

// ── Sources DTOs ─────────────────────────────────────────────────────────────

@Serializable
data class AvailableSourceDto(
    val id: String,
    val name: String,
    @SerialName("displayName") val display_name: String,
    val icon: String,
    val description: String,
    @SerialName("requiresAuth") val requires_auth: Boolean = true
)

@Serializable
data class AvailableSourceListResponse(
    val sources: List<AvailableSourceDto>,
    val total: Int
)

@Serializable
data class SourceResponse(
    val id: String,
    val type: String,
    val sourceId: String,
    val accountName: String? = null,
    val isConnected: Boolean,
    val oauthProviderAccount: String? = null,
    val itemCount: Int = 0,
    val lastSyncedAt: String? = null,
    val nextSyncAt: String? = null,
    val syncStatus: String
)

@Serializable
data class SourceListResponse(
    val sources: List<SourceResponse>,
    val total: Int
)

// ── Brain DTOs ───────────────────────────────────────────────────────────────

@Serializable
data class AIUsageDto(
    @SerialName("prompt_tokens") val prompt_tokens: Int? = 0,
    @SerialName("completion_tokens") val completion_tokens: Int? = 0,
    @SerialName("total_tokens") val total_tokens: Int? = 0
)

@Serializable
data class BrainTodayResponse(
    val priorities: List<String> = emptyList(),
    val blocked: List<String> = emptyList(),
    val action_items: List<String> = emptyList(),
    val suggested_first_step: String = "",
    val model_used: String? = null,
    val provider: String? = null,
    val usage: AIUsageDto? = null,
    val latency_ms: Long? = null
)

@Serializable
data class BrainMissingResponse(
    val missing: List<String> = emptyList(),
    val model_used: String? = null,
    val provider: String? = null,
    val usage: AIUsageDto? = null,
    val latency_ms: Long? = null
)

@Serializable
data class BrainConnectionDto(
    val topic: String,
    val related_sources: List<String>,
    val urgency_score: Int,
    val suggested_action: String,
    val summary: String
)

@Serializable
data class BrainConnectionsResponse(
    val connections: List<BrainConnectionDto> = emptyList(),
    val model_used: String? = null,
    val provider: String? = null,
    val usage: AIUsageDto? = null,
    val latency_ms: Long? = null
)

@Serializable
data class BrainAlertDto(
    val id: String,
    val title: String,
    val message: String,
    val urgency: String,
    val timestamp: String
)

// ── Ingest DTOs ──────────────────────────────────────────────────────────────

@Serializable
data class IngestJobResponse(
    val job_id: String,
    val source_id: String,
    val source_type: String,
    val status: String,
    val progress: Int,
    val message: String,
    val created_at: String,
    val updated_at: String,
    val error: String? = null
)

@Serializable
data class IngestResponse(
    val job_id: String,
    val source_id: String,
    val source_type: String,
    val message: String
)

@Serializable
data class JobStatusResponse(
    val job_id: String,
    @SerialName("source_id") val source_id: String,
    @SerialName("source_type") val source_type: String,
    val status: String,
    val progress: Int,
    val message: String,
    @SerialName("created_at") val created_at: String,
    @SerialName("updated_at") val updated_at: String,
    val error: String? = null
)

@Serializable
data class JobListResponse(
    val jobs: List<JobStatusResponse>,
    val total: Int
)

// ── Chat DTOs ────────────────────────────────────────────────────────────────

@Serializable
data class AskRequest(
    val question: String,
    val use_cache: Boolean = true,
    val model: String? = null
)

@Serializable
data class ChatRequest(val query: String)

@Serializable
data class AskResponse(
    val answer: String,
    val sources: List<SourceCitationDto> = emptyList(),
    @SerialName("model_used") val model_used: String? = null,
    @SerialName("provider") val provider: String? = null,
    @SerialName("usage") val usage: AIUsageDto? = null,
    @SerialName("latency_ms") val latency_ms: Long? = null,
    @SerialName("chunksRetrieved") val chunks_retrieved: Int = 0,
    val cached: Boolean = false,
    val question: String? = null
)

@Serializable
data class QueryHistoryItem(
    val id: String,
    val question: String,
    val answer: String?,
    @SerialName("sources_used") val sources_used: List<SourceCitationDto>? = emptyList(),
    @SerialName("model_used") val model_used: String? = null,
    @SerialName("provider") val provider: String? = null,
    @SerialName("usage") val usage: AIUsageDto? = null,
    @SerialName("latency_ms") val latency_ms: Long? = null,
    @SerialName("created_at") val created_at: String
)

@Serializable
data class QueryHistoryResponse(
    val history: List<QueryHistoryItem>,
    val total: Int
)

@Serializable
data class SourceCitationDto(
    val document_id: String,
    val source_type: String,
    val sender: String? = "",
    val subject: String? = "",
    val timestamp: String? = "",
    val content_preview: String,
    val similarity: Float? = 0f,
    val page_number: Int? = null
)

@Serializable
data class ChatEventDto(
    val type: String,
    val token: String? = null,
    val sources: List<SourceCitationDto>? = null,
    val model: String? = null,
    val provider: String? = null,
    val usage: AIUsageDto? = null,
    @SerialName("latency_ms") val latency_ms: Long? = null,
    @SerialName("finish_reason") val finish_reason: String? = null
)

// ── Saved Items DTOs ─────────────────────────────────────────────────────────

@Serializable
data class SaveItemRequest(
    val item_id: String,
    val item_type: String
)

@Serializable
data class SavedItemResponse(
    val id: String,
    val title: String,
    @SerialName("content") val summary: String? = null,
    @SerialName("item_type") val item_type: String,
    @SerialName("created_at") val saved_at: String,
    @SerialName("item_metadata") val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class SavedListResponse(
    val items: List<SavedItemResponse>,
    val total: Int
)

@Serializable
data class AppSettingsDto(
    val user_name: String,
    val dark_theme: Boolean,
    val pingo_greetings_enabled: Boolean,
    val digest_notifications_enabled: Boolean,
    val sync_frequency_hours: Int,
    val preferred_llm_model: String = "gemini-1.5-pro"
)

// ── Mappers ──────────────────────────────────────────────────────────────────

fun AppSettingsDto.toDomain() = AppSettings(
    userName = user_name,
    darkTheme = dark_theme,
    pingoGreetingsEnabled = pingo_greetings_enabled,
    digestNotificationsEnabled = digest_notifications_enabled,
    syncFrequencyHours = sync_frequency_hours,
    preferredLlmModel = preferred_llm_model
)

fun AppSettings.toDto() = AppSettingsDto(
    user_name = userName,
    dark_theme = darkTheme,
    pingo_greetings_enabled = pingoGreetingsEnabled,
    digest_notifications_enabled = digestNotificationsEnabled,
    sync_frequency_hours = syncFrequencyHours,
    preferred_llm_model = preferredLlmModel
)

fun DigestResponse.toDomain(): DailyDigest {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    
    val derivedDateLabel = date_label ?: date ?: let {
        val month = now.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val day = now.dayOfMonth
        val dayOfWeek = now.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        "$dayOfWeek, $month $day"
    }
    
    val derivedGreeting = greeting ?: let {
        when (now.hour) {
            in 0..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    
    val derivedPingoMessage = pingo_message ?: summary_text ?: "I've analyzed your latest data."
    val derivedTotalCount = total_item_count ?: sections?.sumOf { it.items.size } ?: 0
    
    // If backend sends topics instead of sections, we might need to convert
    val domainSections = if (sections.isNullOrEmpty() && !topics.isNullOrEmpty()) {
        listOf(DigestSection(
            title = "Insights",
            subtitle = "From your knowledge base",
            items = emptyList(), 
            actionItems = action_items?.map { ActionItem(id = it, text = it, sourceType = SourceType.MANUAL) } ?: emptyList()
        ))
    } else {
        sections?.map { it.toDomain() } ?: emptyList()
    }

    return DailyDigest(
        id = id ?: "",
        dateLabel = derivedDateLabel,
        greeting = derivedGreeting,
        pingoMessage = derivedPingoMessage,
        summaryText = summary_text ?: "",
        totalItemCount = derivedTotalCount,
        sections = domainSections,
        topics = topics?.map { it.toDomain() } ?: emptyList(),
        metadata = AIMetadata(
            model = model_used,
            provider = provider,
            usage = usage?.toDomain(),
            latencyMs = latency_ms
        )
    )
}

fun DigestSectionDto.toDomain() = DigestSection(
    title = title,
    subtitle = subtitle,
    items = items.map { it.toDomain() },
    actionItems = action_items.map { it.toDomain() }
)

fun KnowledgeItemDto.toDomain() = KnowledgeItem(
    id = id,
    title = title,
    summary = summary,
    sourceType = try { SourceType.valueOf(source_type.uppercase()) } catch (e: Exception) { SourceType.MANUAL },
    sourceName = source_name,
    timestamp = timestamp,
    tags = tags
)

fun ActionItemDto.toDomain() = ActionItem(
    id = id,
    text = text,
    sourceType = try { SourceType.valueOf(source_type.uppercase()) } catch (e: Exception) { SourceType.MANUAL },
    isCompleted = is_completed
)

fun TopicResponse.toDomain() = Topic(
    id = id ?: "",
    title = name ?: "Untitled Topic",
    summary = summary ?: "",
    itemCount = item_count,
    sources = source_types?.map { type ->
        try { SourceType.valueOf(type.uppercase()) } catch (e: Exception) { SourceType.MANUAL }
    } ?: emptyList(),
    lastUpdatedAt = last_refreshed_at ?: created_at ?: "",
    color = TopicColor.TEAL
)

fun SourceResponse.toDomain() = ConnectedSource(
    id = id,
    type = try { SourceType.valueOf(type.uppercase()) } catch (e: Exception) { SourceType.MANUAL },
    accountName = accountName ?: oauthProviderAccount ?: type.replaceFirstChar { it.uppercase() },
    isConnected = isConnected,
    lastSyncedAt = lastSyncedAt,
    itemCount = itemCount
)

fun BrainTodayResponse.toDomain() = BrainToday(
    priorities = priorities,
    blocked = blocked,
    actionItems = action_items,
    suggestedFirstStep = suggested_first_step,
    metadata = AIMetadata(
        model = model_used,
        provider = provider,
        usage = usage?.toDomain(),
        latencyMs = latency_ms
    )
)

fun BrainMissingResponse.toDomain() = BrainMissing(
    missing = missing,
    metadata = AIMetadata(
        model = model_used,
        provider = provider,
        usage = usage?.toDomain(),
        latencyMs = latency_ms
    )
)

fun AIUsageDto.toDomain() = AIUsage(
    promptTokens = prompt_tokens ?: 0,
    completionTokens = completion_tokens ?: 0,
    totalTokens = total_tokens ?: 0
)

fun BrainConnectionDto.toDomain() = BrainConnection(
    topic = topic,
    relatedSources = related_sources,
    urgencyScore = urgency_score,
    suggestedAction = suggested_action,
    summary = summary
)

fun BrainAlertDto.toDomain() = BrainAlert(
    id = id,
    title = title,
    message = message,
    urgency = urgency,
    timestamp = timestamp
)

fun JobStatusResponse.toDomain() = IngestJob(
    id = job_id,
    sourceId = source_id,
    sourceType = source_type,
    status = status,
    progress = progress,
    message = message,
    createdAt = created_at,
    updatedAt = updated_at,
    error = error
)

fun AskResponse.toDomain(id: String, sentAt: Long) = ChatMessage.Pingo(
    id = id,
    text = answer,
    sentAt = sentAt,
    sources = sources.map { ChatSource(it.source_name_fallback(), it.source_type_to_domain()) },
    isStreaming = false,
    metadata = AIMetadata(
        model = model_used,
        provider = provider,
        usage = usage?.toDomain(),
        latencyMs = latency_ms,
        cached = cached,
        sourcesRetrieved = chunks_retrieved
    )
)

fun SourceCitationDto.source_name_fallback(): String {
    return sender?.takeIf { it.isNotBlank() } ?: subject?.takeIf { it.isNotBlank() } ?: "Source"
}

fun SourceCitationDto.source_type_to_domain(): SourceType {
    return try { SourceType.valueOf(source_type.uppercase()) } catch (e: Exception) { SourceType.MANUAL }
}

fun SavedItemResponse.toDomain() = SavedItem(
    id = id,
    title = title,
    summary = summary ?: "",
    type = item_type,
    savedAt = saved_at,
    metadata = metadata
)

fun AvailableSourceDto.toDomain() = AvailableSource(
    id = id,
    name = name,
    displayName = display_name,
    icon = icon,
    description = description,
    requiresAuth = requires_auth
)

fun PdfUploadResponse.toDomain() = UploadedPdf(
    id = id ?: document_id ?: "",
    filename = filename ?: "document.pdf",
    pages = pages ?: 0,
    status = status ?: when {
        chunks_created != null -> "indexed"
        job_id != null -> "processing"
        else -> "processing"
    },
    uploadedAt = uploaded_at ?: "Just now",
    fileSize = file_size ?: 0,
    errorMessage = error_message
)

// ── PDF Upload DTOs ──────────────────────────────────────────────────────────

@Serializable
data class PdfUploadResponse(
    val message: String? = null,
    val document_id: String? = null,
    val job_id: String? = null,
    val chunks_created: Int? = null,
    val id: String? = null,
    val filename: String? = null,
    val pages: Int? = null,
    val status: String? = null,
    val uploaded_at: String? = null,
    val file_size: Long? = null,
    val error_message: String? = null
)

@Serializable
data class PdfListResponse(
    val pdfs: List<PdfUploadResponse>,
    val total: Int
)

@Serializable
data class PdfDeleteRequest(
    val pdf_id: String
)
