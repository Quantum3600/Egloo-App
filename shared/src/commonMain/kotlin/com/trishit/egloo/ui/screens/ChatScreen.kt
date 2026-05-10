package com.trishit.egloo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.trishit.egloo.domain.models.*
import com.trishit.egloo.domain.viewmodels.*
import com.trishit.egloo.ui.components.*
import org.koin.compose.koinInject
import kotlinx.datetime.*
import kotlin.time.Instant
import org.jetbrains.compose.resources.painterResource
import egloo.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingoScreen(
    viewModel: ChatViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(Res.drawable.pingo_hi),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Pingo", style = MaterialTheme.typography.headlineLarge)
                            Text("AI KNOWLEDGE ASSISTANT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChanged = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val maxWidth = maxWidth
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        maxWidth = maxWidth,
                        onSave = { viewModel.saveMessage(message.id) }
                    )
                }
                
                if (state.isTyping && state.messages.none { it is ChatMessage.Pingo && it.isStreaming }) {
                    item { TypingIndicator() }
                }

                if (state.messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.pingo_hi),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp)
                            )
                            
                            Text(
                                "How can I help you today?",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            
                            if (state.suggestions.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    state.suggestions.forEach { suggestion ->
                                        SuggestionChip(
                                            onClick = { viewModel.sendMessage(suggestion) },
                                            label = { Text(suggestion) },
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    maxWidth: Dp,
    onSave: () -> Unit = {}
) {
    val isUser = message is ChatMessage.User
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    }
    
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val shape = if (isUser) {
        RoundedCornerShape(24.dp, 24.dp, 4.dp, 24.dp)
    } else {
        RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            shape = shape,
            border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) else null,
            tonalElevation = if (isUser) 4.dp else 0.dp,
            modifier = Modifier.widthIn(min = 60.dp, max = maxWidth * 0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    if ((message is ChatMessage.Pingo) && message.isStreaming && message.text.isEmpty()) {
                        Image(
                            painter = painterResource(Res.drawable.pingo_think),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Pingo is thinking...",
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = message.text,
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                if (message is ChatMessage.Pingo) {
                    if (message.sources.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sources:",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            message.sources.forEach { source ->
                                SourceTag(source)
                            }
                        }
                    }

                    // Metadata (Integrity Check)
                    if (!message.isStreaming && message.metadata != null) {
                        val meta = message.metadata
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                meta.model?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }
                                meta.provider?.let {
                                    Text(
                                        text = "via $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (meta.sourcesRetrieved > 0) {
                                    Text(
                                        text = "${meta.sourcesRetrieved} sources",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }
                                meta.usage?.let {
                                    Text(
                                        text = "${it.totalTokens} tokens",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }
                                meta.latencyMs?.let {
                                    Text(
                                        text = "${it}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(message.sentAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!isUser) {
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { 
                        println("ChatScreen: Save clicked for ${message.id}")
                        onSave() 
                    }, 
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.BookmarkBorder, 
                        contentDescription = "Save to Knowledge Base",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                TextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = { Text("Ask Pingo anything...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(28.dp),
                    maxLines = 5
                )
                
                Spacer(Modifier.width(12.dp))
                
                FloatingActionButton(
                    onClick = onSend,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun SourceTag(source: ChatSource) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = source.label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp),
        modifier = Modifier.padding(end = 60.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.pingo_think),
                contentDescription = null,
                modifier = Modifier.size(28.dp).offset(y = dy.dp)
            )
            Text(
                "Pingo is thinking...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(sentAt: Long): String {
    val instant = Instant.fromEpochMilliseconds(sentAt)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
}
