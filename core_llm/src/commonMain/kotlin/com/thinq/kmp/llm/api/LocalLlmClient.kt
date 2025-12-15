package com.thinq.kmp.llm.api

import com.thinq.kmp.llm.error.LlmError
import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic interface for Local LLM operations.
 * 
 * This interface abstracts Android (Gemini Nano/ML Kit) and iOS (Apple Foundation Models)
 * into a single, unified API for on-device AI features.
 * 
 * Usage:
 * ```kotlin
 * val client = LocalLlmClientFactory.create()
 * 
 * // Check capabilities
 * if (LlmCapability.TEXT_GENERATION in client.capabilities) {
 *     val response = client.generateText(
 *         LlmRequest(prompt = "Summarize this text: ...")
 *     )
 *     println(response.text)
 * }
 * ```
 */
interface LocalLlmClient {
    
    /**
     * Capabilities supported by this LLM implementation.
     * Feature code should check capabilities before calling specific methods.
     */
    val capabilities: Set<LlmCapability>
    
    /**
     * Checks if the LLM model is available and ready to use.
     * 
     * @return true if the model is available, false otherwise
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * Generates text from a prompt (non-streaming).
     * 
     * This is a suspend function that blocks until the full response is ready.
     * For incremental results, use [streamText] instead.
     * 
     * @param request The LLM request configuration
     * @return The complete response
     * @throws LlmError.ModelNotAvailable if the model is not available
     * @throws LlmError.SafetyBlocked if the request was blocked by safety filters
     * @throws LlmError.InternalError for other errors
     */
    suspend fun generateText(request: LlmRequest): LlmResponse
    
    /**
     * Generates text with streaming (incremental) output.
     * 
     * Returns a Flow that emits text chunks as they are generated.
     * Not all platforms may support streaming - check [capabilities] for [LlmCapability.STREAMING].
     * 
     * @param request The LLM request configuration
     * @return Flow of text chunks
     * @throws LlmError.ModelNotAvailable if the model is not available
     * @throws LlmError.SafetyBlocked if the request was blocked by safety filters
     */
    fun streamText(request: LlmRequest): Flow<String>
}
