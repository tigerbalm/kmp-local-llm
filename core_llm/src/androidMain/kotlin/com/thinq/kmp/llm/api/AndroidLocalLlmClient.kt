package com.thinq.kmp.llm.api

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.thinq.kmp.llm.error.LlmError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Android implementation of LocalLlmClient using ML Kit GenAI (Gemini Nano).
 *
 * This implementation wraps Google's ML Kit Generative AI API to provide
 * on-device text generation on Android devices.
 *
 * Requirements:
 * - Android API 24+
 * - Gemini Nano model downloaded (via AICore)
 * - ML Kit GenAI dependency in build.gradle
 *
 * @property context Android application context
 */
class AndroidLocalLlmClient(
    private val context: Context
) : LocalLlmClient {

    override val capabilities: Set<LlmCapability> = setOf(
        LlmCapability.TEXT_GENERATION,
        LlmCapability.SUMMARIZATION,
        LlmCapability.REWRITING,
        LlmCapability.STREAMING
    )

    /**
     * ML Kit GenerativeModel instance for Gemini Nano.
     * Uses on-device model with safety settings.
     */
    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-nano",
            apiKey = "", // Not needed for on-device model
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 512
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE)
            )
        )
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            // Check if Gemini Nano model is available
            // This is a simplified check - in production, you should:
            // 1. Check AICore service availability
            // 2. Verify model download status
            // 3. Check device compatibility

            // Attempt a simple generation to verify availability
            val testResponse = generativeModel.generateContent("test")
            testResponse.text != null
        } catch (e: Exception) {
            // Model not available or not downloaded
            false
        }
    }

    override suspend fun generateText(request: LlmRequest): LlmResponse {
        return try {
            // Build the prompt with system instruction if provided
            val fullPrompt = buildString {
                if (request.systemInstruction != null) {
                    append(request.systemInstruction)
                    append("\n\n")
                }
                append(request.prompt)
            }

            // Generate content with custom config
            val response = generativeModel.generateContent(fullPrompt)

            // Extract text from response
            val text = response.text ?: throw LlmError.InternalError("Empty response from model")

            // ML Kit GenAI doesn't provide token usage info directly
            // We estimate based on text length
            val estimatedTokens = estimateTokenCount(text)

            LlmResponse(
                text = text,
                usage = TokenUsage(
                    promptTokens = estimateTokenCount(request.prompt),
                    completionTokens = estimatedTokens,
                    totalTokens = estimateTokenCount(request.prompt) + estimatedTokens
                ),
                raw = response
            )
        } catch (e: com.google.ai.client.generativeai.type.GoogleGenerativeAIException) {
            throw mapGeminiException(e)
        } catch (e: Exception) {
            throw mapException(e)
        }
    }

    override fun streamText(request: LlmRequest): Flow<String> {
        // Build the prompt with system instruction if provided
        val fullPrompt = buildString {
            if (request.systemInstruction != null) {
                append(request.systemInstruction)
                append("\n\n")
            }
            append(request.prompt)
        }

        return generativeModel.generateContentStream(fullPrompt)
            .map { chunk -> chunk.text ?: "" }
            .catch { e ->
                when (e) {
                    is com.google.ai.client.generativeai.type.GoogleGenerativeAIException ->
                        throw mapGeminiException(e)
                    else ->
                        throw mapException(e as? Exception ?: Exception(e.message))
                }
            }
    }

    /**
     * Maps Gemini-specific exceptions to LlmError.
     */
    private fun mapGeminiException(e: com.google.ai.client.generativeai.type.GoogleGenerativeAIException): LlmError {
        return when {
            e.message?.contains("not available", ignoreCase = true) == true ||
            e.message?.contains("not found", ignoreCase = true) == true ->
                LlmError.ModelNotAvailable("Gemini Nano model is not available. Please download it from AICore.")

            e.message?.contains("safety", ignoreCase = true) == true ||
            e.message?.contains("blocked", ignoreCase = true) == true ->
                LlmError.SafetyBlocked("Content blocked by safety filters: ${e.message}")

            e.message?.contains("quota", ignoreCase = true) == true ||
            e.message?.contains("limit", ignoreCase = true) == true ->
                LlmError.InternalError("Rate limit exceeded", e)

            else ->
                LlmError.InternalError("Gemini API error: ${e.message}", e)
        }
    }

    /**
     * Maps platform-specific exceptions to LlmError.
     */
    private fun mapException(e: Exception): LlmError {
        return when {
            e.message?.contains("not available", ignoreCase = true) == true ->
                LlmError.ModelNotAvailable(e.message ?: "Model not available")

            e.message?.contains("safety", ignoreCase = true) == true ->
                LlmError.SafetyBlocked(e.message ?: "Content blocked by safety filters")

            e.message?.contains("permission", ignoreCase = true) == true ->
                LlmError.PermissionDenied(e.message ?: "Permission denied")

            else ->
                LlmError.InternalError(e.message ?: "Internal error", e)
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
