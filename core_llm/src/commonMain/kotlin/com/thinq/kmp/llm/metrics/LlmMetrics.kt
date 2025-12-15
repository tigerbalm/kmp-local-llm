package com.thinq.kmp.llm.metrics

import com.thinq.kmp.llm.api.LlmCapability
import com.thinq.kmp.llm.error.LlmError

/**
 * Interface for reporting LLM metrics and observability data.
 * 
 * Implementations can send metrics to analytics platforms, logging systems,
 * or custom monitoring solutions.
 * 
 * Usage:
 * ```kotlin
 * val client = LocalLlmClientFactory.create()
 * val metricsClient = MetricsEnabledLlmClient(client, MyMetricsReporter())
 * ```
 */
interface LlmMetricsReporter {
    
    /**
     * Called when an LLM request starts.
     * 
     * @param capability The capability being used
     * @param metadata Additional metadata from the request
     */
    fun onRequestStart(
        capability: LlmCapability,
        metadata: Map<String, String>
    )
    
    /**
     * Called when an LLM request succeeds.
     * 
     * @param capability The capability that was used
     * @param durationMs Time taken in milliseconds
     * @param tokenCount Optional token usage information
     */
    fun onRequestSuccess(
        capability: LlmCapability,
        durationMs: Long,
        tokenCount: Int? = null
    )
    
    /**
     * Called when an LLM request fails.
     * 
     * @param capability The capability that was attempted
     * @param error The error that occurred
     * @param durationMs Time taken before failure (in milliseconds)
     */
    fun onRequestFailure(
        capability: LlmCapability,
        error: LlmError,
        durationMs: Long
    )
    
    /**
     * Called when model availability is checked.
     * 
     * @param isAvailable Whether the model is available
     */
    fun onAvailabilityCheck(isAvailable: Boolean)
}

/**
 * No-op implementation of LlmMetricsReporter.
 * Use this when you don't need metrics.
 */
object NoOpMetricsReporter : LlmMetricsReporter {
    override fun onRequestStart(capability: LlmCapability, metadata: Map<String, String>) {}
    override fun onRequestSuccess(capability: LlmCapability, durationMs: Long, tokenCount: Int?) {}
    override fun onRequestFailure(capability: LlmCapability, error: LlmError, durationMs: Long) {}
    override fun onAvailabilityCheck(isAvailable: Boolean) {}
}

/**
 * Console-based metrics reporter for debugging.
 */
class ConsoleMetricsReporter : LlmMetricsReporter {
    override fun onRequestStart(capability: LlmCapability, metadata: Map<String, String>) {
        println("[LLM Metrics] Request started: $capability, metadata=$metadata")
    }
    
    override fun onRequestSuccess(capability: LlmCapability, durationMs: Long, tokenCount: Int?) {
        println("[LLM Metrics] Request succeeded: $capability, duration=${durationMs}ms, tokens=$tokenCount")
    }
    
    override fun onRequestFailure(capability: LlmCapability, error: LlmError, durationMs: Long) {
        println("[LLM Metrics] Request failed: $capability, error=${error.message}, duration=${durationMs}ms")
    }
    
    override fun onAvailabilityCheck(isAvailable: Boolean) {
        println("[LLM Metrics] Availability check: $isAvailable")
    }
}
