package com.trishit.egloo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trishit.egloo.domain.viewmodels.BrainViewModel
import com.trishit.egloo.domain.viewmodels.SourcesViewModel
import com.trishit.egloo.domain.viewmodels.TopicsViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EgloosScreen(
    topicsViewModel: TopicsViewModel = koinInject(),
    sourcesViewModel: SourcesViewModel = koinInject(),
    brainViewModel: BrainViewModel = koinInject(),
    onNavigateToPdfUpload: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Topics", "Connections", "Sources")
    var showCreateTopicDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.titleSmall) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                // Topics Actions
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FloatingActionButton(
                        onClick = { topicsViewModel.generateTopics() },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate Topics")
                    }
                    FloatingActionButton(
                        onClick = { showCreateTopicDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Topic")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> TopicsScreen(topicsViewModel)
                1 -> ConnectionsScreen(brainViewModel)
                2 -> SourcesScreen(
                    viewModel = sourcesViewModel,
                    onNavigateToPdfUpload = onNavigateToPdfUpload
                )
            }
        }
    }

    if (showCreateTopicDialog) {
        CreateTopicDialog(
            onDismiss = { showCreateTopicDialog = false },
            onCreate = { name, summary ->
                topicsViewModel.createTopic(name, summary)
                showCreateTopicDialog = false
            }
        )
    }
}

@Composable
private fun CreateTopicDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Topic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Topic Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Short Summary") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, summary) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel") }
        }
    )
}
