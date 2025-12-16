package com.thinq.kmp.sample

import android.app.Application
import com.thinq.kmp.llm.factory.AndroidContextHolder

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Android context for core_llm
        AndroidContextHolder.initialize(this)
    }
}
