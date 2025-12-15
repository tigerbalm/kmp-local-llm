package com.thinq.kmp.llm.factory

import com.thinq.kmp.llm.api.IosLocalLlmClient
import com.thinq.kmp.llm.api.LocalLlmClient

/**
 * iOS implementation of createPlatformClient.
 */
internal actual fun createPlatformClient(): LocalLlmClient {
    return IosLocalLlmClient()
}
