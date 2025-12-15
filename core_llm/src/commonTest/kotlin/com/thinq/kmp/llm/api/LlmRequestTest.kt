package com.thinq.kmp.llm.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LlmRequestTest {
    
    @Test
    fun `should create valid request with default values`() {
        val request = LlmRequest(prompt = "Hello, world!")
        
        assertEquals("Hello, world!", request.prompt)
        assertEquals(null, request.systemInstruction)
        assertEquals(512, request.maxTokens)
        assertEquals(0.7, request.temperature)
        assertEquals(emptyMap(), request.metadata)
    }
    
    @Test
    fun `should create request with custom values`() {
        val metadata = mapOf("userId" to "123", "sessionId" to "abc")
        val request = LlmRequest(
            prompt = "Summarize this",
            systemInstruction = "You are a helpful assistant",
            maxTokens = 1024,
            temperature = 0.9,
            metadata = metadata
        )
        
        assertEquals("Summarize this", request.prompt)
        assertEquals("You are a helpful assistant", request.systemInstruction)
        assertEquals(1024, request.maxTokens)
        assertEquals(0.9, request.temperature)
        assertEquals(metadata, request.metadata)
    }
    
    @Test
    fun `should reject blank prompt`() {
        assertFailsWith<IllegalArgumentException> {
            LlmRequest(prompt = "")
        }
        
        assertFailsWith<IllegalArgumentException> {
            LlmRequest(prompt = "   ")
        }
    }
    
    @Test
    fun `should reject invalid maxTokens`() {
        assertFailsWith<IllegalArgumentException> {
            LlmRequest(prompt = "test", maxTokens = 0)
        }
        
        assertFailsWith<IllegalArgumentException> {
            LlmRequest(prompt = "test", maxTokens = -1)
        }
    }
    
    @Test
    fun `should reject invalid temperature`() {
        assertFailsWith<IllegalArgumentException> {
            LlmRequest(prompt = "test", temperature = -0.1)
        }
        
        assertFailsWith<IllegalArgumentException> {
            LlmRequest(prompt = "test", temperature = 1.1)
        }
    }
}
