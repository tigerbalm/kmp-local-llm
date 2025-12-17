package com.thinq.kmp.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.thinq.kmp.llm.api.LlmCapability
import com.thinq.kmp.sample.model.ChatUiState
import com.thinq.kmp.sample.model.Message
import com.thinq.kmp.sample.model.PresetPrompt
import com.thinq.kmp.sample.repository.LlmRepository
import com.thinq.kmp.sample.viewmodel.ChatViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KMP Local LLM - Desktop",
        state = rememberWindowState(width = 1000.dp, height = 700.dp)
    ) {
        MaterialTheme {
            ChatApp()
        }
    }
}

@Composable
fun ChatApp() {
    val repository = remember { LlmRepository() }
    val viewModel = remember {
        ChatViewModel(
            repository = repository,
            viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    val supportsStreaming = repository.getCapabilities().contains(LlmCapability.STREAMING)

    ChatScreen(
        uiState = uiState,
        supportsStreaming = supportsStreaming,
        onSendMessage = { text -> viewModel.sendMessage(text) },
        onSendStreamingMessage = { text -> viewModel.sendStreamingMessage(text) },
        onClearChat = { viewModel.clearMessages() }
    )
}

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    supportsStreaming: Boolean,
    onSendMessage: (String) -> Unit,
    onSendStreamingMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.currentStreamingText) {
        if (uiState.messages.isNotEmpty() || uiState.currentStreamingText.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KMP Local LLM - Desktop Demo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.isLlmAvailable) {
                    Text(
                        text = "✓ Mock LLM Ready",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(onClick = onClearChat) {
                    Text("Clear Chat")
                }
            }
        }

        // Preset Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetPrompt.values().forEach { preset ->
                Button(
                    onClick = { inputText = preset.promptTemplate },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(preset.displayName, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.messages) { message ->
                MessageBubble(message)
            }

            // Streaming message
            if (uiState.isStreaming && uiState.currentStreamingText.isNotEmpty()) {
                item {
                    StreamingMessageBubble(uiState.currentStreamingText)
                }
            }
        }

        // Error display
        if (uiState.error != null) {
            Text(
                text = "Error: ${uiState.error}",
                color = Color.Red,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Input area
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                enabled = !uiState.isLoading && !uiState.isStreaming,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.White
                )
            )

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = !uiState.isLoading && !uiState.isStreaming && inputText.isNotBlank()
            ) {
                Text("Send")
            }

            if (supportsStreaming) {
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendStreamingMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = !uiState.isLoading && !uiState.isStreaming && inputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF03DAC5))
                ) {
                    Text("Stream", color = Color.Black)
                }
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.isUser) Color(0xFF6200EE) else Color.White,
            elevation = 2.dp,
            modifier = Modifier.widthIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = if (message.isUser) Color.White else Color.Black
                )

                if (message.durationMs != null || message.tokenCount != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (message.durationMs != null) {
                            Text(
                                text = "⏱ ${message.durationMs}ms",
                                fontSize = 10.sp,
                                color = if (message.isUser) Color.White.copy(alpha = 0.7f)
                                else Color.Gray
                            )
                        }
                        if (message.tokenCount != null) {
                            Text(
                                text = "📊 ${message.tokenCount} tokens",
                                fontSize = 10.sp,
                                color = if (message.isUser) Color.White.copy(alpha = 0.7f)
                                else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingMessageBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            elevation = 2.dp,
            modifier = Modifier.widthIn(max = 600.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(text = text, color = Color.Black)
            }
        }
    }
}

@Preview
@Composable
fun ChatScreenPreview() {
    MaterialTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    Message("1", "Hello!", true),
                    Message("2", "Hi there! How can I help?", false)
                ),
                isLlmAvailable = true
            ),
            supportsStreaming = true,
            onSendMessage = {},
            onSendStreamingMessage = {},
            onClearChat = {}
        )
    }
}
