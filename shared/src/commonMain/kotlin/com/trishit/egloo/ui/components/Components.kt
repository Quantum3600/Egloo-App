package com.trishit.egloo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.trishit.egloo.data.repositories.HealthStatus
import com.trishit.egloo.domain.models.*
import com.trishit.egloo.ui.theme.EglooColors
import org.jetbrains.compose.resources.painterResource
import egloo.shared.generated.resources.*
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

// ── Source Logo Icon (Circular) ──────────────────────────────────────────────

@Composable
fun SourceLogoIcon(type: SourceType, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    val painter = when (type) {
        SourceType.GMAIL -> painterResource(Res.drawable.gmail)
        SourceType.SLACK -> painterResource(Res.drawable.slack)
        SourceType.GOOGLE_DRIVE, SourceType.DRIVE -> painterResource(Res.drawable.drive)
        SourceType.NOTION -> painterResource(Res.drawable.notion)
        SourceType.PDF -> painterResource(Res.drawable.pdf)
        else -> null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = type.displayName,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(size * 0.6f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
        Icon(Icons.Default.Person, "Account")
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
    metadata: AIMetadata? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f, fill = false)
            )
            
            metadata?.model?.let {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
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
            .fillMaxWidth(0.8f)
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

// ── Priority Card (Brain Hero) ───────────────────────────────────────────────

@Composable
fun PriorityCard(
    priorities: List<String>,
    suggestedStep: String,
    metadata: AIMetadata? = null,
    onStepClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Top Priorities",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                metadata?.model?.let {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            priorities.take(3).forEach { priority ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        priority,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            if (suggestedStep.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    onClick = onStepClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "PINGO'S SUGGESTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Text(
                            suggestedStep,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

// ── Alert Item (Horizontal) ──────────────────────────────────────────────────

@Composable
fun AlertItem(alert: BrainAlert, modifier: Modifier = Modifier) {
    val color = when (alert.urgency.lowercase()) {
        "critical" -> MaterialTheme.colorScheme.error
        "high" -> EglooColors.BeakAmber
        else -> MaterialTheme.colorScheme.secondary
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier.width(260.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Ingest Status Indicator ──────────────────────────────────────────────────

@Composable
fun IngestStatusIndicator(job: IngestJob, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                progress = { job.progress / 100f },
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                "Pingo is syncing ${job.sourceType.lowercase()}...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${job.progress}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ── Celery Health Indicator ──────────────────────────────────────────────────

@Composable
fun CeleryHealthIndicator(status: HealthStatus?, modifier: Modifier = Modifier) {
    val isOnline = status?.worker_status == "online" || status?.status == "healthy"
    val color = if (isOnline) EglooColors.TealPrimary else MaterialTheme.colorScheme.error
    val text = if (isOnline) "Pingo Workers: Online" else "Pingo Workers: Offline"
    val icon = if (isOnline) Icons.Default.CheckCircle else Icons.Default.Warning

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            if (status?.services?.isNotEmpty() == true) {
                Spacer(Modifier.weight(1f))
                Text(
                    "Healthy",
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ── Loading animation (Floating Pingo) ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingAnimation(message: String = "Pingo is thinking...") {
    val infiniteTransition = rememberInfiniteTransition()
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.pingo_med),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = dy.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            LoadingIndicator(
                modifier = Modifier.size(24.dp),
                polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Error screen ──────────────────────────────────────────────────────────────

@Composable
fun ErrorScreen(
    title: String = "Oops! The Igloo is a bit chilly.",
    message: String,
    onRetry: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.pingo_error),
                contentDescription = null,
                modifier = Modifier.size(300.dp)
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Try Again")
            }
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
