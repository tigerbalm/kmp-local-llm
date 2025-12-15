package com.thinq.kmp.llm.api

/**
 * Represents the capabilities supported by a Local LLM implementation.
 * 
 * Feature code should check `capabilities.contains(...)` before using specific features.
 */
enum class LlmCapability {
    /**
     * Basic text generation from prompts
     */
    TEXT_GENERATION,

    /**
     * Summarization of long text
     */
    SUMMARIZATION,

    /**
     * Text rewriting and paraphrasing
     */
    REWRITING,

    /**
     * Grammar and spelling correction
     */
    PROOFREADING,

    /**
     * Streaming response support (incremental text generation)
     */
    STREAMING
}
