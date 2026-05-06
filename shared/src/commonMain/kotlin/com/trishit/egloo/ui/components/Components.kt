package com.trishit.egloo.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.trishit.egloo.domain.models.*
import com.trishit.egloo.ui.theme.EglooColors
import kotlin.time.Clock
import kotlin.time.Instant


// ── Source badge ──────────────────────────────────────────────────────────────

@Composable
fun SourceBadge(type: SourceType, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (type) {
        SourceType.GMAIL        -> Triple(Color(0x22EA4335), EglooColors.GmailRed,    "Gmail")
        SourceType.SLACK        -> Triple(Color(0x22611f69), EglooColors.SlackPurple, "Slack")
        SourceType.GOOGLE_DRIVE, SourceType.DRIVE -> Triple(Color(0x221967D2), EglooColors.DriveBlue,   "Drive")
        SourceType.NOTION       -> Triple(Color(0x2237352F), EglooColors.NotionGray,  "Notion")
        SourceType.PDF          -> Triple(Color(0x22FF5722), EglooColors.PdfOrange,   "PDF")
        SourceType.MANUAL       -> Triple(Color(0x22888888), Color(0xFF888888),       "Manual")
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}

// ── Source dot (compact) ──────────────────────────────────────────────────────

@Composable
fun SourceDot(type: SourceType, modifier: Modifier = Modifier) {
    val color = when (type) {
        SourceType.GMAIL        -> EglooColors.GmailRed
        SourceType.SLACK        -> EglooColors.SlackPurple
        SourceType.GOOGLE_DRIVE, SourceType.DRIVE -> EglooColors.DriveBlue
        SourceType.NOTION       -> EglooColors.NotionGray
        SourceType.PDF          -> EglooColors.PdfOrange
        SourceType.MANUAL       -> Color(0xFF888888)
    }
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// ── Knowledge item card ───────────────────────────────────────────────────────

@Composable
fun KnowledgeCard(
    item: KnowledgeItem,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Source + time row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SourceDot(item.sourceType)
                Text(
                    text = item.sourceName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = item.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            Spacer(Modifier.height(6.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(4.dp))

            // Summary
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            // Project tag
            if (item.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.tags.forEach { tag ->
                        ProjectTag(tag)
                    }
                }
            }
        }
    }
}

// ── Project tag pill ──────────────────────────────────────────────────────────

@Composable
fun ProjectTag(tag: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

// ── Pingo avatar (used in chat + onboarding) ──────────────────────────────────

@Composable
fun PingoAvatar(size: Dp = 36.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "P",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

// ── Action item row ────────────────────────────────────────────────────────────

@Composable
fun ActionItemRow(action: ActionItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(EglooColors.BeakAmber)
        )
        Text(
            text = action.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Pingo message bubble (onboarding / tips) ──────────────────────────────────

@Composable
fun PingoMessageBubble(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PingoAvatar(size = 32.dp)
        Column {
            Text(
                text = "Pingo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun Instant.timeAgo(): String {
    val now = Clock.System.now()
    val diff = now - this
    val minutes = diff.inWholeMinutes
    val hours = diff.inWholeHours
    val days = diff.inWholeDays
    
    return when {
        minutes < 1L  -> "just now"
        minutes < 60L -> "${minutes}m ago"
        hours < 24L   -> "${hours}h ago"
        else          -> "${days}d ago"
    }
}
