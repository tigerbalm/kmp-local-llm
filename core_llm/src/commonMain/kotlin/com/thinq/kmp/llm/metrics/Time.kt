package com.thinq.kmp.llm.metrics

/**
 * Platform-agnostic time measurement.
 * Returns current time in milliseconds.
 */
internal expect fun currentTimeMillis(): Long
