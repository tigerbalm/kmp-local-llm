package com.thinq.kmp.llm.metrics

import com.thinq.kmp.llm.api.LlmCapability
import com.thinq.kmp.llm.api.LlmRequest
import com.thinq.kmp.llm.api.LlmResponse
import com.thinq.kmp.llm.api.LocalLlmClient
import com.thinq.kmp.llm.error.LlmError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * Decorator that adds metrics reporting to any LocalLlmClient implementation.
 * 
 * Usage:
 * ```kotlin
 * val baseClient = LocalLlmClientFactory.create()
 * val client = MetricsEnabledLlmClient(
 *     delegate = baseClient,
 *     reporter = ConsoleMetricsReporter()
 * )
 * ```
 */
class MetricsEnabledLlmClient(
    private val delegate: LocalLlmClient,
    private val reporter: LlmMetricsReporter
) : LocalLlmClient {
    
    override val capabilities: Set<LlmCapability>
        get() = delegate.capabilities
    
    override suspend fun isAvailable(): Boolean {
        val result = delegate.isAvailable()
        reporter.onAvailabilityCheck(result)
        return result
    }
    
    override suspend fun generateText(request: LlmRequest): LlmResponse {
        val startTime = currentTimeMillis()
        reporter.onRequestStart(LlmCapability.TEXT_GENERATION, request.metadata)
        
        return try {
            val response = delegate.generateText(request)
            val duration = currentTimeMillis() - startTime
            
            reporter.onRequestSuccess(
                capability = LlmCapability.TEXT_GENERATION,
                durationMs = duration,
                tokenCount = response.usage?.totalTokens
            )
            
            response
        } catch (e: LlmError) {
            val duration = currentTimeMillis() - startTime
            reporter.onRequestFailure(
                capability = LlmCapability.TEXT_GENERATION,
                error = e,
                durationMs = duration
            )
            throw e
        }
    }
    
    override fun streamText(request: LlmRequest): Flow<String> {
        val startTime = currentTimeMillis()
        
        return delegate.streamText(request)
            .onStart {
                reporter.onRequestStart(LlmCapability.STREAMING, request.metadata)
            }
            .onCompletion { error ->
                val duration = currentTimeMillis() - startTime
                
                if (error == null) {
                    reporter.onRequestSuccess(
                        capability = LlmCapability.STREAMING,
                        durationMs = duration
                    )
                } else if (error is LlmError) {
                    reporter.onRequestFailure(
                        capability = LlmCapability.STREAMING,
                        error = error,
                        durationMs = duration
                    )
                }
            }
            .catch { error ->
                if (error is LlmError) {
                    throw error
                } else {
                    throw LlmError.InternalError("Streaming error", error)
                }
            }
    }
    
    private fun currentTimeMillis(): Long {
        // Platform-agnostic time measurement
        return kotlin.system.getTimeMillis()
    }
}
