package com.trishit.egloo.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.trishit.egloo.domain.models.*
import com.trishit.egloo.domain.viewmodels.*
import com.trishit.egloo.ui.components.*
import com.trishit.egloo.ui.theme.EglooColors
import org.koin.compose.koinInject

// =============================================================================
// TOPICS SCREEN
// =============================================================================

@Composable
fun TopicsScreen(viewModel: TopicsViewModel = koinInject()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text("Topics", style = MaterialTheme.typography.displaySmall)
            Text(
                "${state.topics.size} clusters from your knowledge base",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.isLoading || state.isGenerating) {
            LoadingAnimation(if (state.isGenerating) "Clustering your knowledge..." else "Pingo is thinking...")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.topics) { topic ->
                    TopicCard(
                        topic = topic,
                        onClick = { viewModel.selectTopic(topic) })
                }
            }
        }
    }

    // Topic detail bottom sheet
    state.selectedTopic?.let { topic ->
        TopicDetailSheet(
            topic = topic,
            onDismiss = { viewModel.selectTopic(null) },
        )
    }
}

@Composable
internal fun TopicCard(topic: Topic, onClick: () -> Unit) {
    val accentColor = topic.color.toColor()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth().aspectRatio(1.1f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Source dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                topic.sources.take(3).forEach {
                    SourceDot(it)
                }
            }

            Column {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${topic.itemCount} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                )
                Text(
                    text = "Updated ${topic.lastUpdatedAt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicDetailSheet(topic: Topic, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(topic.title, style = MaterialTheme.typography.headlineMedium)
            Text(topic.summary, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                topic.sources.forEach {
                    SourceBadge(it)
                }
            }
            Text(
                "${topic.itemCount} items · Updated ${topic.lastUpdatedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun TopicColor.toColor(): Color = when (this) {
    TopicColor.TEAL   -> EglooColors.TealPrimary
    TopicColor.BLUE   -> EglooColors.BlueAccent
    TopicColor.AMBER  -> EglooColors.BeakAmber
    TopicColor.CORAL  -> Color(0xFFD85A30)
    TopicColor.PURPLE -> Color(0xFF7F77DD)
}

// =============================================================================
// CONNECTIONS SCREEN
// =============================================================================

@Composable
fun ConnectionsScreen(viewModel: BrainViewModel = koinInject()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text("Semantic Connections", style = MaterialTheme.typography.displaySmall)
            Text(
                "How your different data sources relate to each other",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.isLoading) {
            LoadingAnimation("Pingo is analyzing connections...")
        } else if (state.connections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No cross-source connections found yet.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.connections) { connection ->
                    ConnectionCard(connection)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ConnectionCard(connection: BrainConnection) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(EglooColors.TealPrimary)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        connection.topic,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    connection.relatedSources.forEach { sourceName ->
                        val sourceType = try { SourceType.valueOf(sourceName.uppercase()) } catch (e: Exception) { SourceType.MANUAL }
                        SourceDot(sourceType)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(connection.summary, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        connection.suggestedAction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

// =============================================================================
// SOURCES SCREEN
// =============================================================================

@Composable
fun SourcesScreen(
    viewModel: SourcesViewModel = koinInject(),
    ingestViewModel: IngestViewModel = koinInject(),
    onNavigateToPdfUpload: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val ingestState by ingestViewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateToPdfUpload) {
        if (state.navigateToPdfUpload) {
            onNavigateToPdfUpload()
            viewModel.onPdfUploadNavigated()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sources", style = MaterialTheme.typography.displaySmall)
                    Text(
                        "Connect your tools so Pingo can read them",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                Button(
                    onClick = { ingestViewModel.triggerSyncAll() },
                    enabled = ingestState.activeJobs.isEmpty(),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    if (ingestState.activeJobs.isNotEmpty()) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sync All")
                    }
                }
            }
        }

        item { CeleryHealthIndicator(ingestState.healthStatus) }

        item { Spacer(Modifier.height(8.dp)) }

        // Show active ingestion jobs
        if (ingestState.activeJobs.isNotEmpty()) {
            items(ingestState.activeJobs) { job ->
                IngestStatusIndicator(job)
                Spacer(Modifier.height(8.dp))
            }
        }

        // Show auth message if present
        if (state.authMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (state.authMessageType == AuthMessageType.SUCCESS) {
                        EglooColors.TealSurface
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = if (state.authMessageType == AuthMessageType.SUCCESS) {
                                EglooColors.TealDark
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Text(
                            state.authMessage ?: "",
                            color = if (state.authMessageType == AuthMessageType.SUCCESS) {
                                EglooColors.TealDark
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }
            }
        }

        // Render merged source rows
        items(state.sourceRows) { row ->
            SourceRowWithAvailable(
                sourceRow = row,
                isConnecting = state.connectingSourceId == row.sourceId,
                onConnect = { viewModel.connectSource(row.sourceId) },
                onDisconnect = { viewModel.disconnectSource(row.sourceId) },
                onSync = { row.connectedSource?.id?.let { ingestViewModel.triggerSourceSync(it) } },
                isSyncing = ingestState.activeJobs.any { it.sourceId == row.connectedSource?.id }
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SourceRowWithAvailable(
    sourceRow: SourceRowData,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit,
    isSyncing: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SourceLogoIcon(
                when (sourceRow.sourceId) {
                    "gmail" -> SourceType.GMAIL
                    "slack" -> SourceType.SLACK
                    "google_drive" -> SourceType.GOOGLE_DRIVE
                    "notion" -> SourceType.NOTION
                    "pdf" -> SourceType.PDF
                    else -> SourceType.MANUAL
                },
                size = 36.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sourceRow.availableSource.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (sourceRow.isConnected) {
                        "${sourceRow.itemCount} items · synced ${sourceRow.lastSyncedAt ?: "never"}"
                    } else {
                        sourceRow.availableSource.description
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                isConnecting -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                sourceRow.isConnected -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isSyncing) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = onSync,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Sync",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        OutlinedButton(
                            onClick = onDisconnect,
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                        ) {
                            Text("Disconnect", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                else -> Button(
                    onClick = onConnect,
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Text("Connect", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// =============================================================================
// SETTINGS SCREEN
// =============================================================================

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinInject(),
    onRestartOnboarding: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.displaySmall)
        }

        item {
            state.userProfile?.let { profile ->
                SettingsSection("Account") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PingoAvatar(size = 40.dp)
                        Column {
                            Text(profile.full_name ?: "Egloo User", style = MaterialTheme.typography.titleMedium)
                            Text(profile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            SettingsSection("Appearance") {
                ToggleRow(
                    label = "Dark theme",
                    description = "Night mode — Pingo prefers the dark",
                    checked = settings.darkTheme,
                    onCheckedChange = viewModel::toggleDarkTheme,
                )
            }
        }

        item {
            SettingsSection("Pingo") {
                ToggleRow(
                    label = "Morning greetings",
                    description = "Pingo says hi when you open the app",
                    checked = settings.pingoGreetingsEnabled,
                    onCheckedChange = viewModel::togglePingoGreetings,
                )
            }
        }

        item {
            SettingsSection("Sync") {
                ToggleRow(
                    label = "Digest notifications",
                    description = "Get notified when your daily digest is ready",
                    checked = settings.digestNotificationsEnabled,
                    onCheckedChange = viewModel::toggleDigestNotifications,
                )
                Spacer(Modifier.height(8.dp))
                SyncFrequencyRow(
                    hours = settings.syncFrequencyHours,
                    onSelect = viewModel::setSyncFrequency,
                )
            }
        }

        item {
            SettingsSection("Debug") {
                Button(
                    onClick = onRestartOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Restart Onboarding")
                }
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }
        }

        item {
            if (state.isSaved) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EglooColors.TealSurface,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Check, null, tint = EglooColors.TealDark)
                        Text("Settings saved", color = EglooColors.TealDark)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary),
        )
    }
}

@Composable
private fun SyncFrequencyRow(hours: Int, onSelect: (Int) -> Unit) {
    Column {
        Text("Sync every", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 3, 6, 12, 24).forEach { h ->
                FilterChip(
                    selected = hours == h,
                    onClick = { onSelect(h) },
                    label = { Text("${h}h") },
                )
            }
        }
    }
}

// =============================================================================
// SAVED ITEMS SCREEN
// =============================================================================

@Composable
fun SavedItemsScreen(
    viewModel: SavedViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Saved Items",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Your bookmarked insights",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingAnimation("Pingo is fetching your saved items...")
            }
        } else if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No saved items yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.items) { item ->
                    SavedItemCard(item, onRemove = { viewModel.unsaveItem(item.id) })
                }
            }
        }
    }
}

@Composable
private fun SavedItemCard(item: SavedItem, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.type.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
