package com.thinq.kmp.llm.factory

import com.thinq.kmp.llm.api.LocalLlmClient
import com.thinq.kmp.llm.api.MockLocalLlmClient

/**
 * JVM/Desktop implementation of createPlatformClient.
 * Returns a mock implementation for testing and development.
 */
internal actual fun createPlatformClient(): LocalLlmClient {
    return MockLocalLlmClient()
}
