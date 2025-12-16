package com.thinq.kmp.sample.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun MainScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
