package com.thinq.kmp.llm.api

import com.thinq.kmp.llm.error.LlmError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS implementation of LocalLlmClient using Apple Foundation Models.
 *
 * This implementation bridges to Swift's AppleLocalLlmBridge which wraps
 * Apple's LanguageModels framework.
 *
 * Requirements:
 * - iOS 18.1+
 * - Apple Intelligence enabled
 * - LanguageModels framework available
 */
class IosLocalLlmClient : LocalLlmClient {

    // Bridge to Swift implementation
    private val bridge = AppleLocalLlmBridge(maxTokens = 512, temperature = 0.7)

    override val capabilities: Set<LlmCapability> = setOf(
        LlmCapability.TEXT_GENERATION,
        LlmCapability.SUMMARIZATION,
        LlmCapability.REWRITING,
        LlmCapability.PROOFREADING,
        LlmCapability.STREAMING  // iOS supports streaming via AsyncSequence
    )

    override suspend fun isAvailable(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            // Check availability via Swift bridge
            val available = bridge.isAvailable()

            if (available) {
                // Also try to prepare session to ensure it really works
                bridge.prepareWithCompletion { error ->
                    if (error != null) {
                        continuation.resume(false)
                    } else {
                        continuation.resume(true)
                    }
                }
            } else {
                continuation.resume(false)
            }
        }
    }

    override suspend fun generateText(request: LlmRequest): LlmResponse {
        return suspendCancellableCoroutine { continuation ->
            // Call Swift bridge
            bridge.generateWithPrompt(
                prompt = request.prompt,
                systemInstruction = request.systemInstruction
            ) { result, error ->
                if (error != null) {
                    continuation.resumeWithException(mapNSError(error))
                } else if (result != null) {
                    // Estimate token usage
                    val estimatedTokens = estimateTokenCount(result)

                    val response = LlmResponse(
                        text = result,
                        usage = TokenUsage(
                            promptTokens = estimateTokenCount(request.prompt),
                            completionTokens = estimatedTokens,
                            totalTokens = estimateTokenCount(request.prompt) + estimatedTokens
                        )
                    )
                    continuation.resume(response)
                } else {
                    continuation.resumeWithException(
                        LlmError.InternalError("Empty response from Apple Foundation Models")
                    )
                }
            }
        }
    }

    override fun streamText(request: LlmRequest): Flow<String> = callbackFlow {
        // Call Swift bridge streaming API
        bridge.generateStreamWithPrompt(
            prompt = request.prompt,
            systemInstruction = request.systemInstruction,
            onChunk = { chunk ->
                // Send each chunk to the flow
                trySend(chunk).isSuccess
            },
            onComplete = { error ->
                if (error != null) {
                    close(mapNSError(error))
                } else {
                    close()
                }
            }
        )

        awaitClose {
            // Cleanup if needed
            bridge.cleanup()
        }
    }

    /**
     * Maps iOS NSError to LlmError.
     */
    private fun mapNSError(error: NSError): LlmError {
        val errorMessage = error.localizedDescription

        return when (error.code.toInt()) {
            1 -> LlmError.ModelNotAvailable("iOS 18.1+ required for Apple Intelligence")
            2 -> LlmError.ModelNotAvailable("LanguageModels framework not available")
            3 -> LlmError.InternalError("Session not initialized")
            else -> when {
                errorMessage.contains("not available", ignoreCase = true) ||
                errorMessage.contains("18.1", ignoreCase = true) ->
                    LlmError.ModelNotAvailable(errorMessage)

                errorMessage.contains("permission", ignoreCase = true) ->
                    LlmError.PermissionDenied(errorMessage)

                errorMessage.contains("safety", ignoreCase = true) ||
                errorMessage.contains("blocked", ignoreCase = true) ->
                    LlmError.SafetyBlocked(errorMessage)

                else ->
                    LlmError.InternalError(errorMessage)
            }
        }
    }

    /**
     * Estimates token count based on text length.
     * Rule of thumb: ~4 characters per token for English text.
     */
    private fun estimateTokenCount(text: String): Int {
        return (text.length / 4).coerceAtLeast(1)
    }
}
