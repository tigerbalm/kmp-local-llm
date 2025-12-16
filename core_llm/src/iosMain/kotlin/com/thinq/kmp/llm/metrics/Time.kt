package com.thinq.kmp.llm.metrics

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
