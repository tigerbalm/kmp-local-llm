package com.thinq.kmp.llm.factory

import com.thinq.kmp.llm.api.LocalLlmClient

/**
 * Factory for creating platform-specific LocalLlmClient instances.
 * 
 * Usage:
 * ```kotlin
 * val client = LocalLlmClientFactory.create()
 * ```
 */
object LocalLlmClientFactory {
    
    /**
     * Creates a platform-specific LocalLlmClient instance.
     * 
     * @return Platform-specific implementation (AndroidLocalLlmClient or IosLocalLlmClient)
     */
    fun create(): LocalLlmClient {
        return createPlatformClient()
    }
}

/**
 * Platform-specific factory method.
 * Implemented in androidMain and iosMain.
 */
internal expect fun createPlatformClient(): LocalLlmClient
