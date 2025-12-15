package com.thinq.kmp.llm.factory

import android.content.Context
import com.thinq.kmp.llm.api.AndroidLocalLlmClient
import com.thinq.kmp.llm.api.LocalLlmClient

/**
 * Android application context holder.
 * Must be initialized before using LocalLlmClientFactory.
 */
object AndroidContextHolder {
    private var appContext: Context? = null
    
    /**
     * Initialize with application context.
     * Call this in your Application.onCreate()
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
    
    internal fun getContext(): Context {
        return appContext ?: throw IllegalStateException(
            "AndroidContextHolder not initialized. Call AndroidContextHolder.initialize(context) first."
        )
    }
}

/**
 * Android implementation of createPlatformClient.
 */
internal actual fun createPlatformClient(): LocalLlmClient {
    val context = AndroidContextHolder.getContext()
    return AndroidLocalLlmClient(context)
}
