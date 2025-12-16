package com.thinq.kmp.sample.model

import com.thinq.kmp.sample.util.currentTimeMillis

data class Message(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = currentTimeMillis(),
    val tokenCount: Int? = null,
    val durationMs: Long? = null,
    val isStreaming: Boolean = false
)
