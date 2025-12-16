package com.thinq.kmp.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thinq.kmp.sample.model.Message
import com.thinq.kmp.sample.model.PresetPrompt
import com.thinq.kmp.sample.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KMP LLM Sample") },
                actions = {
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Banner
            if (!uiState.isLlmAvailable) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "LLM not available on this device",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Error Banner
            uiState.error?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Messages List
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageItem(message)
                }

                // Streaming message
                if (uiState.isStreaming && uiState.currentStreamingText.isNotEmpty()) {
                    item {
                        StreamingMessageItem(uiState.currentStreamingText)
                    }
                }
            }

            // Auto-scroll to bottom
            LaunchedEffect(uiState.messages.size, uiState.currentStreamingText) {
                if (uiState.messages.isNotEmpty() || uiState.currentStreamingText.isNotEmpty()) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(
                            listState.layoutInfo.totalItemsCount
                        )
                    }
                }
            }

            // Preset Prompts
            if (uiState.messages.isEmpty()) {
                PresetPromptsRow(
                    onPresetSelected = { /* Will be used with input */ }
                )
            }

            // Input Area
            InputArea(
                onSendMessage = { text ->
                    viewModel.sendMessage(text)
                },
                onSendStreamingMessage = { text ->
                    viewModel.sendStreamingMessage(text)
                },
                enabled = uiState.isLlmAvailable && !uiState.isLoading && !uiState.isStreaming
            )
        }
    }
}

@Composable
fun MessageItem(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge
                )

                // Metrics
                if (!message.isUser && (message.tokenCount != null || message.durationMs != null)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            message.tokenCount?.let { append("$it tokens") }
                            if (message.tokenCount != null && message.durationMs != null) {
                                append(" • ")
                            }
                            message.durationMs?.let { append("${it}ms") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun StreamingMessageItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PresetPromptsRow(onPresetSelected: (PresetPrompt) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(PresetPrompt.entries.toTypedArray()) { preset ->
            AssistChip(
                onClick = { onPresetSelected(preset) },
                label = { Text(preset.displayName) }
            )
        }
    }
}

@Composable
fun InputArea(
    onSendMessage: (String) -> Unit,
    onSendStreamingMessage: (String) -> Unit,
    enabled: Boolean
) {
    var text by remember { mutableStateOf("") }
    var useStreaming by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Divider()

        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ask something...") },
                    enabled = enabled,
                    maxLines = 4
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useStreaming,
                        onCheckedChange = { useStreaming = it },
                        enabled = enabled
                    )
                    Text(
                        text = "Use streaming",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (text.isNotBlank()) {
                        if (useStreaming) {
                            onSendStreamingMessage(text)
                        } else {
                            onSendMessage(text)
                        }
                        text = ""
                    }
                },
                modifier = Modifier.padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}
