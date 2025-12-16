package com.thinq.kmp.sample.viewmodel

import com.thinq.kmp.llm.api.LlmCapability
import com.thinq.kmp.sample.model.ChatUiState
import com.thinq.kmp.sample.model.Message
import com.thinq.kmp.sample.model.PresetPrompt
import com.thinq.kmp.sample.repository.LlmRepository
import com.thinq.kmp.sample.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class ChatViewModel(
    private val repository: LlmRepository,
    private val viewModelScope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        checkLlmAvailability()
    }

    private fun checkLlmAvailability() {
        viewModelScope.launch {
            val isAvailable = repository.isAvailable()
            _uiState.update { it.copy(isLlmAvailable = isAvailable) }

            if (!isAvailable) {
                _uiState.update {
                    it.copy(error = "LLM is not available on this device. Please ensure the model is downloaded.")
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Add user message
        val userMessage = Message(
            id = generateId(),
            text = text,
            isUser = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isLoading = true,
                error = null
            )
        }

        // Generate response
        viewModelScope.launch {
            val startTime = currentTimeMillis()
            val result = repository.generateText(text)
            val duration = currentTimeMillis() - startTime

            result.fold(
                onSuccess = { response ->
                    val aiMessage = Message(
                        id = generateId(),
                        text = response.text,
                        isUser = false,
                        tokenCount = response.usage?.totalTokens,
                        durationMs = duration
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + aiMessage,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun sendStreamingMessage(text: String) {
        if (text.isBlank()) return
        if (!repository.getCapabilities().contains(LlmCapability.STREAMING)) {
            sendMessage(text)
            return
        }

        // Add user message
        val userMessage = Message(
            id = generateId(),
            text = text,
            isUser = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isStreaming = true,
                currentStreamingText = "",
                error = null
            )
        }

        // Stream response
        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            val startTime = currentTimeMillis()
            var fullText = ""

            repository.streamText(text)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isStreaming = false,
                            currentStreamingText = "",
                            error = error.message
                        )
                    }
                }
                .onCompletion {
                    val duration = currentTimeMillis() - startTime
                    if (fullText.isNotEmpty()) {
                        val aiMessage = Message(
                            id = generateId(),
                            text = fullText,
                            isUser = false,
                            durationMs = duration
                        )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + aiMessage,
                                isStreaming = false,
                                currentStreamingText = ""
                            )
                        }
                    }
                }
                .collect { chunk ->
                    fullText += chunk
                    _uiState.update {
                        it.copy(currentStreamingText = fullText)
                    }
                }
        }
    }

    fun sendPresetPrompt(preset: PresetPrompt, userText: String) {
        val fullPrompt = preset.promptTemplate + userText
        sendMessage(fullPrompt)
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                messages = emptyList(),
                error = null
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun cancelStreaming() {
        streamingJob?.cancel()
        _uiState.update {
            it.copy(
                isStreaming = false,
                currentStreamingText = ""
            )
        }
    }

    private fun generateId(): String {
        return "${currentTimeMillis()}-${Random.nextInt()}"
    }
}
