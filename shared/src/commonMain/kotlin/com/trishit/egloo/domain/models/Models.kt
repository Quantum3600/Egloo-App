package com.trishit.egloo.domain.models

import kotlinx.serialization.Serializable

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
    val id: String = "",
    val dateLabel: String,
    val greeting: String,
    val pingoMessage: String,
    val summaryText: String = "",
    val totalItemCount: Int,
    val sections: List<DigestSection>,
    val topics: List<Topic> = emptyList()
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
    abstract val sentAt: Long // Using epoch milliseconds for serialization

    @Serializable
    data class User(
        override val id: String,
        override val text: String,
        override val sentAt: Long
    ) : ChatMessage()

    @Serializable
    data class Pingo(
        override val id: String,
        override val text: String,
        override val sentAt: Long,
        val sources: List<ChatSource> = emptyList(),
        val isStreaming: Boolean = false,
        val modelUsed: String? = null,
        val sourcesRetrieved: Int = 0
    ) : ChatMessage()
}

@Serializable
data class ChatSource(
    val label: String,
    val type: SourceType
)

@Serializable
data class SavedItem(
    val id: String,
    val title: String,
    val summary: String,
    val type: String, // "digest", "query", "topic", "item"
    val savedAt: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class AppSettings(
    val userName: String = "User",
    val darkTheme: Boolean = false,
    val pingoGreetingsEnabled: Boolean = true,
    val digestNotificationsEnabled: Boolean = true,
    val syncFrequencyHours: Int = 4
)

// ─────────────────────────────────────────────
// Available and Connected Sources
// ─────────────────────────────────────────────

@Serializable
data class AvailableSource(
    val id: String,           // "gmail", "slack", "google_drive"
    val name: String,         // "Gmail", "Slack"
    val displayName: String,  // "Gmail" (for UI)
    val icon: String,         // URL to icon
    val description: String,  // "Read emails and attachments"
    val requiresAuth: Boolean = true
)

@Serializable
data class SourceRowData(
    val availableSource: AvailableSource,
    val connectedSource: ConnectedSource?,
    val isConnected: Boolean = connectedSource?.isConnected ?: false,
    val itemCount: Int = connectedSource?.itemCount ?: 0,
    val accountName: String = connectedSource?.accountName ?: "",
    val lastSyncedAt: String? = connectedSource?.lastSyncedAt,
    val sourceId: String = availableSource.id
)
