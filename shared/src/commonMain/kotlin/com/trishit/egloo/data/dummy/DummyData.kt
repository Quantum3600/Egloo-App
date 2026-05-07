package com.trishit.egloo.data.dummy

import com.trishit.egloo.domain.models.*


object DummyData {

    val connectedSources = listOf(
        ConnectedSource(
            id = "src_gmail",
            type = SourceType.GMAIL,
            accountName = "work.email@gmail.com",
            isConnected = true,
            lastSyncedAt = "2025-05-02T08:00:00Z",
            itemCount = 342
        ),
        ConnectedSource(
            id = "src_slack",
            type = SourceType.SLACK,
            accountName = "Egloo Workspace",
            isConnected = true,
            lastSyncedAt = "2025-05-02T09:15:00Z",
            itemCount = 1204
        ),
        ConnectedSource(
            id = "src_drive",
            type = SourceType.DRIVE,
            accountName = "Personal Drive",
            isConnected = false,
            lastSyncedAt = null,
            itemCount = 0
        ),
        ConnectedSource(
            id = "src_notion",
            type = SourceType.NOTION,
            accountName = "Knowledge Base",
            isConnected = false,
            lastSyncedAt = null,
            itemCount = 0
        ),
    )

    val dummyDigest = DailyDigest(
        id = "d_today",
        dateLabel = "Friday, May 2",
        greeting = "Good morning, User",
        pingoMessage = "I've analyzed 12 new messages since yesterday. The database migration is the hot topic in Slack today.",
        totalItemCount = 1546,
        sections = listOf(
            DigestSection(
                title = "Critical Updates",
                subtitle = "Items requiring your attention",
                items = listOf(
                    KnowledgeItem(
                        id = "d1",
                        title = "Database migration decision",
                        summary = "Team agreed to defer migration to v2 schema until after June release. Sarah flagged 3 blocking issues in #backend.",
                        sourceType = SourceType.SLACK,
                        sourceName = "Slack · #backend",
                        timestamp = "2h ago",
                        tags = listOf("Project Alpha", "Engineering")
                    ),
                    KnowledgeItem(
                        id = "d2",
                        title = "Client feedback on designs",
                        summary = "Nexus Corp approved the dashboard mockups. They want a dark mode variant before the May 15 presentation.",
                        sourceType = SourceType.GMAIL,
                        sourceName = "Gmail · Nexus Corp",
                        timestamp = "4h ago",
                        tags = listOf("Client", "Design")
                    )
                ),
                actionItems = listOf(
                    ActionItem("a1", "Review Sarah's blockers in #backend", SourceType.SLACK),
                    ActionItem("a2", "Send dark mode mockups to Nexus Corp", SourceType.GMAIL)
                )
            ),
            DigestSection(
                title = "Recent Knowledge",
                subtitle = "Information Pingo saved for you",
                items = listOf(
                    KnowledgeItem(
                        id = "d3",
                        title = "Q2 roadmap doc updated",
                        summary = "Alex added the AI search feature to Q3 scope. Analytics dashboard moved to Q4.",
                        sourceType = SourceType.DRIVE,
                        sourceName = "Drive · Roadmap 2025",
                        timestamp = "Yesterday",
                        tags = listOf("Planning")
                    )
                )
            )
        )
    )

    val dummyTopics = listOf(
        Topic(
            id = "t1",
            title = "Project Alpha",
            summary = "Active sprint, 2 blockers. Client review on May 15.",
            itemCount = 14,
            sources = listOf(SourceType.SLACK, SourceType.GMAIL, SourceType.DRIVE),
            lastUpdatedAt = "2h ago",
            color = TopicColor.TEAL
        ),
        Topic(
            id = "t2",
            title = "Database Migration",
            summary = "Deferred to post-June release. 3 open issues.",
            itemCount = 7,
            sources = listOf(SourceType.SLACK, SourceType.NOTION),
            lastUpdatedAt = "2h ago",
            color = TopicColor.BLUE
        ),
        Topic(
            id = "t3",
            title = "Q2 Planning",
            summary = "Roadmap updated. AI search moved to Q3.",
            itemCount = 11,
            sources = listOf(SourceType.DRIVE, SourceType.GMAIL),
            lastUpdatedAt = "Yesterday",
            color = TopicColor.AMBER
        )
    )

    val dummySettings = AppSettings(
        userName = "User",
        darkTheme = true,
        pingoGreetingsEnabled = true,
        digestNotificationsEnabled = true,
        syncFrequencyHours = 4
    )

    val availableSources = listOf(
        AvailableSource(
            id = "gmail",
            name = "Gmail",
            displayName = "Gmail",
            icon = "https://www.gstatic.com/images/branding/product/1x/gmail_2020q4_512dp.png",
            description = "Read your emails and attachments",
            requiresAuth = true
        ),
        AvailableSource(
            id = "slack",
            name = "Slack",
            displayName = "Slack",
            icon = "https://a.slack-edge.com/80588/marketing/img/icons/icon_slack_hash_colored.png",
            description = "Sync messages and files from Slack",
            requiresAuth = true
        ),
        AvailableSource(
            id = "google_drive",
            name = "Google Drive",
            displayName = "Google Drive",
            icon = "https://www.gstatic.com/images/branding/product/1x/drive_2020q4_512dp.png",
            description = "Index documents and PDFs",
            requiresAuth = true
        ),
        AvailableSource(
            id = "notion",
            name = "Notion",
            displayName = "Notion",
            icon = "https://upload.wikimedia.org/wikipedia/commons/e/e9/Notion-logo.svg",
            description = "Connect to your Notion workspace",
            requiresAuth = true
        ),
        AvailableSource(
            id = "pdf",
            name = "PDF Upload",
            displayName = "Upload PDFs",
            icon = "https://www.adobe.com/content/dam/cc/icons/pdf.svg",
            description = "Upload PDF documents manually",
            requiresAuth = false
        )
    )
}
