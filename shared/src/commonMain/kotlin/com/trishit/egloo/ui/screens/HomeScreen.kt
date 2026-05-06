package com.trishit.egloo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinInject(),
    onItemClick: (KnowledgeItem) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    when {
        state.isLoading -> LoadingState()
        state.error != null -> ErrorState(state.error!!) { viewModel.loadDigest() }
        state.digest != null -> HomeContent(
            digest = state.digest!!,
            onItemClick = onItemClick,
        )
    }
}

@Composable
private fun HomeContent(
    digest: DailyDigest,
    onItemClick: (KnowledgeItem) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {

        // ── Greeting ──────────────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = digest.dateLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${digest.greeting} ✦",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        // ── Pingo message bubble ───────────────────────────────────────────────
        item {
            PingoMessageBubble(digest.pingoMessage)
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
                StatChip(
                    "${digest.sections.size} topics",
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
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(
                "Pingo is reading your messages…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
