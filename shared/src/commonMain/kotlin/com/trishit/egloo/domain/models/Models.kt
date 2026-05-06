package com.trishit.egloo.domain.models

import kotlinx.serialization.Serializable
import kotlin.time.Instant

// ─────────────────────────────────────────────
//  Core domain models
// ─────────────────────────────────────────────

enum class SourceType(val displayName: String) {
    GMAIL("Gmail"),
    SLACK("Slack"),
    DRIVE("Google Drive"),
    NOTION("Notion"),
    PDF("PDF Document"),
    GOOGLE_DRIVE("Google Drive"),
    MANUAL("Manual Entry")
}

@Serializable
data class ConnectedSource(
    val id: String,
    val type: SourceType,
    val accountName: String,
    val isConnected: Boolean,
    val lastSyncedAt: String? = null,
    val itemCount: Int = 0
)

@Serializable
data class DailyDigest(
    val dateLabel: String,
    val greeting: String,
    val pingoMessage: String,
    val totalItemCount: Int,
    val sections: List<DigestSection>
)

@Serializable
data class DigestSection(
    val title: String,
    val subtitle: String,
    val items: List<KnowledgeItem>,
    val actionItems: List<ActionItem> = emptyList()
)

@Serializable
data class KnowledgeItem(
    val id: String,
    val title: String,
    val summary: String,
    val sourceType: SourceType,
    val sourceName: String,
    val timestamp: String,
    val tags: List<String> = emptyList()
)

@Serializable
data class ActionItem(
    val id: String,
    val text: String,
    val sourceType: SourceType,
    val isCompleted: Boolean = false
)

enum class TopicColor { TEAL, BLUE, AMBER, CORAL, PURPLE }

@Serializable
data class Topic(
    val id: String,
    val title: String,
    val summary: String,
    val itemCount: Int,
    val sources: List<SourceType>,
    val lastUpdatedAt: String,
    val color: TopicColor = TopicColor.TEAL
)

@Serializable
sealed class ChatMessage {
    abstract val id: String
    abstract val text: String
    abstract val sentAt: Instant

    @Serializable
    data class User(
        override val id: String,
        override val text: String,
        override val sentAt: Instant
    ) : ChatMessage()

    @Serializable
    data class Pingo(
        override val id: String,
        override val text: String,
        override val sentAt: Instant,
        val sources: List<ChatSource> = emptyList(),
        val isStreaming: Boolean = false
    ) : ChatMessage()
}

@Serializable
data class ChatSource(
    val label: String,
    val type: SourceType
)

@Serializable
data class AppSettings(
    val userName: String = "User",
    val darkTheme: Boolean = false,
    val pingoGreetingsEnabled: Boolean = true,
    val digestNotificationsEnabled: Boolean = true,
    val syncFrequencyHours: Int = 4
)
