package com.thinq.kmp.llm.metrics

import com.thinq.kmp.llm.api.LlmCapability
import com.thinq.kmp.llm.api.LlmRequest
import com.thinq.kmp.llm.api.LlmResponse
import com.thinq.kmp.llm.api.LocalLlmClient
import com.thinq.kmp.llm.api.TokenUsage
import com.thinq.kmp.llm.error.LlmError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMetricsReporter : LlmMetricsReporter {
    val events = mutableListOf<String>()
    
    override fun onRequestStart(capability: LlmCapability, metadata: Map<String, String>) {
        events.add("start:$capability")
    }
    
    override fun onRequestSuccess(capability: LlmCapability, durationMs: Long, tokenCount: Int?) {
        events.add("success:$capability:${durationMs}ms:$tokenCount")
    }
    
    override fun onRequestFailure(capability: LlmCapability, error: LlmError, durationMs: Long) {
        events.add("failure:$capability:${error.javaClass.simpleName}")
    }
    
    override fun onAvailabilityCheck(isAvailable: Boolean) {
        events.add("availability:$isAvailable")
    }
}

class TestLocalLlmClient : LocalLlmClient {
    override val capabilities = setOf(LlmCapability.TEXT_GENERATION)
    
    override suspend fun isAvailable() = true
    
    override suspend fun generateText(request: LlmRequest): LlmResponse {
        return LlmResponse(
            text = "Test response",
            usage = TokenUsage(totalTokens = 100)
        )
    }
    
    override fun streamText(request: LlmRequest): Flow<String> {
        return flowOf("chunk1", "chunk2")
    }
}

class MetricsEnabledLlmClientTest {
    
    @Test
    fun `should report availability check`() = runTest {
        val reporter = TestMetricsReporter()
        val client = MetricsEnabledLlmClient(TestLocalLlmClient(), reporter)
        
        client.isAvailable()
        
        assertEquals(1, reporter.events.size)
        assertEquals("availability:true", reporter.events[0])
    }
    
    @Test
    fun `should report successful generation`() = runTest {
        val reporter = TestMetricsReporter()
        val client = MetricsEnabledLlmClient(TestLocalLlmClient(), reporter)
        
        val response = client.generateText(LlmRequest("test"))
        
        assertEquals("Test response", response.text)
        assertTrue(reporter.events.size >= 2)
        assertEquals("start:TEXT_GENERATION", reporter.events[0])
        assertTrue(reporter.events[1].startsWith("success:TEXT_GENERATION:"))
        assertTrue(reporter.events[1].contains(":100"))
    }
}
