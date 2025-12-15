package com.thinq.kmp.llm.api

/**
 * Response model for LLM text generation.
 * 
 * @property text The generated text
 * @property usage Token usage information (if available from platform)
 * @property raw Raw platform-specific response object (for debugging/advanced use)
 */
data class LlmResponse(
    val text: String,
    val usage: TokenUsage? = null,
    val raw: Any? = null
)

/**
 * Token usage information.
 * 
 * @property promptTokens Number of tokens in the prompt
 * @property completionTokens Number of tokens in the completion
 * @property totalTokens Total tokens used
 */
data class TokenUsage(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null
)
