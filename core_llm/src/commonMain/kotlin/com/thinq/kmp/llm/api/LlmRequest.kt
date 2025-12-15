package com.thinq.kmp.llm.api

/**
 * Request model for LLM text generation.
 * 
 * @property prompt The main prompt/input text
 * @property systemInstruction Optional system-level instruction to guide the model's behavior
 * @property maxTokens Maximum number of tokens to generate (default: 512)
 * @property temperature Controls randomness in generation. Higher values = more creative (0.0 to 1.0)
 * @property metadata Additional metadata for tracking/logging purposes
 */
data class LlmRequest(
    val prompt: String,
    val systemInstruction: String? = null,
    val maxTokens: Int = 512,
    val temperature: Double = 0.7,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(prompt.isNotBlank()) { "Prompt cannot be blank" }
        require(maxTokens > 0) { "maxTokens must be positive" }
        require(temperature in 0.0..1.0) { "temperature must be between 0.0 and 1.0" }
    }
}
