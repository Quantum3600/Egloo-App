package com.trishit.egloo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.*
import com.trishit.egloo.domain.models.*
import com.trishit.egloo.domain.viewmodels.*
import com.trishit.egloo.ui.components.*
import com.trishit.egloo.ui.theme.EglooColors
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.painterResource
import egloo.shared.generated.resources.Res
import egloo.shared.generated.resources.pingo_hi

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = koinInject(),
    brainViewModel: BrainViewModel = koinInject(),
    ingestViewModel: IngestViewModel = koinInject(),
    settingsViewModel: SettingsViewModel = koinInject(),
    onItemClick: (KnowledgeItem) -> Unit = {},
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val brainState by brainViewModel.uiState.collectAsState()
    val ingestState by ingestViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    
    val userName = settingsState.settings.userName

    when {
        homeState.isLoading && homeState.digest == null -> LoadingAnimation("Pingo is reading your messages...")
        homeState.error != null && homeState.digest == null -> ErrorScreen(message = homeState.error!!) { homeViewModel.loadDigest() }
        else -> HomeContent(
            userName = userName,
            digest = homeState.digest,
            isLoading = homeState.isLoading,
            brainState = brainState,
            ingestState = ingestState,
            onItemClick = onItemClick,
            onRegenerate = { homeViewModel.loadDigest(force = true) },
            onSaveDigest = { homeViewModel.saveDigest(it) }
        )
    }
}

@Composable
private fun HomeContent(
    userName: String,
    digest: DailyDigest?,
    isLoading: Boolean,
    brainState: BrainUiState,
    ingestState: IngestUiState,
    onItemClick: (KnowledgeItem) -> Unit,
    onRegenerate: () -> Unit,
    onSaveDigest: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {

        // ── Greeting ──────────────────────────────────────────────────────────
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = digest?.dateLabel ?: "Today",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val firstName = if (userName == "User") "" else userName.split(" ").firstOrNull() ?: ""
                    val greetingBase = digest?.greeting?.replace(", User", "") ?: "Good morning"
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (firstName.isNotEmpty()) "$greetingBase, $firstName ✦" else "$greetingBase ✦",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Image(
                            painter = painterResource(Res.drawable.pingo_hi),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                
                if (isLoading && digest != null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    IconButton(onClick = onRegenerate) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ── Ingest Status (if any) ──────────────────────────────────────────
        if (ingestState.activeJobs.isNotEmpty()) {
            items(ingestState.activeJobs) { job ->
                IngestStatusIndicator(job)
            }
        }

        // ── Brain Priorities ──────────────────────────────────────────────────
        brainState.today?.let { today ->
            item {
                PriorityCard(
                    priorities = today.priorities,
                    suggestedStep = today.suggestedFirstStep,
                    metadata = today.metadata
                )
            }
        }

        // ── Urgent Alerts ─────────────────────────────────────────────────────
        if (brainState.alerts.isNotEmpty()) {
            item {
                Text(
                    "Action Required",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(brainState.alerts) { alert ->
                        AlertItem(alert)
                    }
                }
            }
        }

        // ── Missing Items ─────────────────────────────────────────────────────
        brainState.missing?.let { missingData ->
            if (missingData.missing.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Pending Items",
                        subtitle = "Things that might need your attention",
                        metadata = missingData.metadata
                    )
                }
                items(missingData.missing) { itemText ->
                    ActionItemRow(ActionItem(id = "", text = itemText, sourceType = SourceType.MANUAL))
                }
            }
        }

        // ── Pingo message bubble ───────────────────────────────────────────────
        digest?.pingoMessage?.let {
            item {
                PingoMessageBubble(it)
            }
        }

        // ── Daily Recap Divider ───────────────────────────────────────────────
        if (digest != null) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Daily Recap",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    digest.metadata?.model?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    IconButton(onClick = { digest.id.let { onSaveDigest(it) } }) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Save Digest")
                    }
                }
            }

            // ── Stats row ─────────────────────────────────────────────────────────
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StatChip(
                        "${digest.totalItemCount} items read",
                        modifier = Modifier.weight(1f)
                    )
                    val topicCount = if (digest.topics.isNotEmpty()) digest.topics.size else digest.sections.size
                    StatChip(
                        "$topicCount topics",
                        modifier = Modifier.weight(1f)
                    )
                    val totalActions = digest.sections.sumOf { it.actionItems.size }
                    StatChip(
                        "$totalActions actions",
                        highlight = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Digest sections ───────────────────────────────────────────────────
            digest.sections.forEach { section ->
                item {
                    SectionHeader(
                        title = section.title,
                        subtitle = section.subtitle,
                    )
                }

                // Action items special display
                if (section.actionItems.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            section.actionItems.forEach { action ->
                                ActionItemRow(action)
                            }
                        }
                    }
                }

                items(section.items) { item ->
                    KnowledgeCard(
                        item = item,
                        onClick = { onItemClick(item) })
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun StatChip(label: String, highlight: Boolean = false, modifier: Modifier = Modifier) {
    val backgroundColor = if (highlight) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (highlight) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun LoadingState() {
    LoadingAnimation("Pingo is reading your messages…")
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    ErrorScreen(message = message, onRetry = onRetry)
}
