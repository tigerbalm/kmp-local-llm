package com.thinq.kmp.llm.error

/**
 * Base exception class for Local LLM errors.
 * 
 * Provides fail-safe error handling across platforms.
 */
sealed class LlmError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * The LLM model is not available on this device.
     * Could be due to: model not downloaded, device incompatibility, or platform restrictions.
     */
    class ModelNotAvailable(message: String = "LLM model is not available on this device") : LlmError(message)
    
    /**
     * Permission denied to use the LLM feature.
     * User may need to enable AI features in system settings.
     */
    class PermissionDenied(message: String = "Permission denied to use LLM") : LlmError(message)
    
    /**
     * Request was blocked by safety filters.
     * Content may violate safety policies.
     */
    class SafetyBlocked(message: String = "Request blocked by safety filters") : LlmError(message)
    
    /**
     * Internal error occurred during LLM processing.
     */
    class InternalError(message: String = "Internal LLM error", cause: Throwable? = null) : LlmError(message, cause)
    
    /**
     * Request parameters are invalid.
     */
    class InvalidRequest(message: String = "Invalid request parameters") : LlmError(message)
    
    /**
     * Request timeout.
     */
    class Timeout(message: String = "LLM request timeout") : LlmError(message)
}
