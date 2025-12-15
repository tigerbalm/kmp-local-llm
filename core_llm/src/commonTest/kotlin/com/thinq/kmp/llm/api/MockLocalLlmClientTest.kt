package com.thinq.kmp.llm.api

import com.thinq.kmp.llm.error.LlmError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Mock implementation for testing
 */
class MockLocalLlmClient(
    private val available: Boolean = true,
    private val shouldFail: Boolean = false
) : LocalLlmClient {
    
    override val capabilities = setOf(
        LlmCapability.TEXT_GENERATION,
        LlmCapability.STREAMING
    )
    
    override suspend fun isAvailable(): Boolean = available
    
    override suspend fun generateText(request: LlmRequest): LlmResponse {
        if (shouldFail) {
            throw LlmError.InternalError("Mock error")
        }
        
        return LlmResponse(
            text = "Mock response to: ${request.prompt}",
            usage = TokenUsage(
                promptTokens = 10,
                completionTokens = 20,
                totalTokens = 30
            )
        )
    }
    
    override fun streamText(request: LlmRequest): Flow<String> = flow {
        if (shouldFail) {
            throw LlmError.InternalError("Mock streaming error")
        }
        
        emit("Mock ")
        emit("streaming ")
        emit("response")
    }
}

class MockLocalLlmClientTest {
    
    @Test
    fun `should check availability`() = runTest {
        val client = MockLocalLlmClient(available = true)
        assertTrue(client.isAvailable())
    }
    
    @Test
    fun `should generate text successfully`() = runTest {
        val client = MockLocalLlmClient()
        val request = LlmRequest(prompt = "Test prompt")
        
        val response = client.generateText(request)
        
        assertEquals("Mock response to: Test prompt", response.text)
        assertEquals(10, response.usage?.promptTokens)
        assertEquals(20, response.usage?.completionTokens)
        assertEquals(30, response.usage?.totalTokens)
    }
    
    @Test
    fun `should handle generation failure`() = runTest {
        val client = MockLocalLlmClient(shouldFail = true)
        val request = LlmRequest(prompt = "Test")
        
        assertFailsWith<LlmError.InternalError> {
            client.generateText(request)
        }
    }
    
    @Test
    fun `should stream text successfully`() = runTest {
        val client = MockLocalLlmClient()
        val request = LlmRequest(prompt = "Test")
        
        val chunks = client.streamText(request).toList()
        
        assertEquals(3, chunks.size)
        assertEquals("Mock ", chunks[0])
        assertEquals("streaming ", chunks[1])
        assertEquals("response", chunks[2])
    }
    
    @Test
    fun `should handle streaming failure`() = runTest {
        val client = MockLocalLlmClient(shouldFail = true)
        val request = LlmRequest(prompt = "Test")
        
        assertFailsWith<LlmError.InternalError> {
            client.streamText(request).toList()
        }
    }
}
