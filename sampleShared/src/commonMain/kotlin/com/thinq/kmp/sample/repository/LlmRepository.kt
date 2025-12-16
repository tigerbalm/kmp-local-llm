package com.thinq.kmp.sample.repository

import com.thinq.kmp.llm.api.LlmRequest
import com.thinq.kmp.llm.api.LlmResponse
import com.thinq.kmp.llm.api.LocalLlmClient
import com.thinq.kmp.llm.error.LlmError
import com.thinq.kmp.llm.factory.LocalLlmClientFactory
import com.thinq.kmp.llm.metrics.ConsoleMetricsReporter
import com.thinq.kmp.llm.metrics.MetricsEnabledLlmClient
import kotlinx.coroutines.flow.Flow

class LlmRepository {
    private val baseClient: LocalLlmClient = LocalLlmClientFactory.create()
    private val client: LocalLlmClient = MetricsEnabledLlmClient(
        delegate = baseClient,
        reporter = ConsoleMetricsReporter()
    )

    suspend fun isAvailable(): Boolean {
        return try {
            client.isAvailable()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generateText(
        prompt: String,
        systemInstruction: String? = null,
        maxTokens: Int = 512,
        temperature: Double = 0.7
    ): Result<LlmResponse> {
        return try {
            val request = LlmRequest(
                prompt = prompt,
                systemInstruction = systemInstruction,
                maxTokens = maxTokens,
                temperature = temperature
            )
            val response = client.generateText(request)
            Result.success(response)
        } catch (e: LlmError.ModelNotAvailable) {
            Result.failure(Exception("LLM model is not available on this device"))
        } catch (e: LlmError.SafetyBlocked) {
            Result.failure(Exception("Content blocked by safety filters"))
        } catch (e: LlmError.PermissionDenied) {
            Result.failure(Exception("Permission denied"))
        } catch (e: LlmError) {
            Result.failure(Exception("LLM error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }

    fun streamText(
        prompt: String,
        systemInstruction: String? = null,
        maxTokens: Int = 512,
        temperature: Double = 0.7
    ): Flow<String> {
        val request = LlmRequest(
            prompt = prompt,
            systemInstruction = systemInstruction,
            maxTokens = maxTokens,
            temperature = temperature
        )
        return client.streamText(request)
    }

    fun getCapabilities() = client.capabilities
}
