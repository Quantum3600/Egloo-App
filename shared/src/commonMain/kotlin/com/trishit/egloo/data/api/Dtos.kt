package com.trishit.egloo.data.api

import com.trishit.egloo.domain.models.*
import kotlinx.serialization.Serializable

// ── Auth DTOs ───────────────────────────────────────────────────────────────

@Serializable
data class UserRegisterRequest(
    val email: String,
    val password: String,
    val full_name: String
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
    val token_type: String
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val full_name: String,
    val is_active: Boolean
)

// ── Digest DTOs ──────────────────────────────────────────────────────────────

@Serializable
data class DigestResponse(
    val id: String,
    val date_label: String,
    val greeting: String,
    val pingo_message: String,
    val total_item_count: Int,
    val sections: List<DigestSectionDto>
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
    val id: String,
    val title: String,
    val summary: String,
    val item_count: Int,
    val sources: List<String>,
    val last_updated_at: String,
    val color: String
)

// ── Sources DTOs ─────────────────────────────────────────────────────────────

@Serializable
data class ConnectedSourceDto(
    val id: String,
    val type: String,
    val account_name: String,
    val is_connected: Boolean,
    val last_synced_at: String? = null,
    val item_count: Int = 0
)

// ── Chat DTOs ────────────────────────────────────────────────────────────────

@Serializable
data class AskRequest(
    val question: String
)

@Serializable
data class AskResponse(
    val answer: String,
    val citations: List<CitationDto> = emptyList()
)

@Serializable
data class CitationDto(
    val source_id: String,
    val source_name: String,
    val snippet: String
)

@Serializable
data class ChatChunkDto(
    val delta: String,
    val done: Boolean,
    val citations: List<CitationDto>? = null
)

// ── Mappers ──────────────────────────────────────────────────────────────────

fun DigestResponse.toDomain() = DailyDigest(
    dateLabel = date_label,
    greeting = greeting,
    pingoMessage = pingo_message,
    totalItemCount = total_item_count,
    sections = sections.map { it.toDomain() }
)

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
    sourceType = SourceType.valueOf(source_type.uppercase()),
    sourceName = source_name,
    timestamp = timestamp,
    tags = tags
)

fun ActionItemDto.toDomain() = ActionItem(
    id = id,
    text = text,
    sourceType = SourceType.valueOf(source_type.uppercase()),
    isCompleted = is_completed
)

fun TopicResponse.toDomain() = Topic(
    id = id,
    title = title,
    summary = summary,
    itemCount = item_count,
    sources = sources.map { SourceType.valueOf(it.uppercase()) },
    lastUpdatedAt = last_updated_at,
    color = try { TopicColor.valueOf(color.uppercase()) } catch (e: Exception) { TopicColor.TEAL }
)

fun ConnectedSourceDto.toDomain() = ConnectedSource(
    id = id,
    type = SourceType.valueOf(type.uppercase()),
    accountName = account_name,
    isConnected = is_connected,
    lastSyncedAt = last_synced_at,
    itemCount = item_count
)
