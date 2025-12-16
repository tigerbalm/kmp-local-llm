package com.thinq.kmp.sample.model

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isLlmAvailable: Boolean = false,
    val error: String? = null,
    val currentStreamingText: String = "",
    val isStreaming: Boolean = false
)
