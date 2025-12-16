package com.studymate.app.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Rate limiter for API requests
 * Enforces Gemini free tier limits: 10 requests per minute
 */
class RateLimiter(
    private val maxRequestsPerMinute: Int = 10
) {
    private val requestTimestamps = mutableListOf<Long>()
    private val mutex = Mutex()
    
    /**
     * Execute a block while respecting rate limits
     */
    suspend fun <T> acquire(block: suspend () -> T): T {
        mutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            val oneMinuteAgo = now - 60_000
            
            // Remove timestamps older than 1 minute
            requestTimestamps.removeAll { it < oneMinuteAgo }
            
            // Wait if we've hit the limit
            if (requestTimestamps.size >= maxRequestsPerMinute) {
                val oldestRequest = requestTimestamps.first()
                val waitTime = 60_000 - (now - oldestRequest)
                if (waitTime > 0) {
                    delay(waitTime)
                }
                // Clear old timestamps after waiting
                requestTimestamps.clear()
            }
            
            requestTimestamps.add(Clock.System.now().toEpochMilliseconds())
        }
        
        return block()
    }
    
    /**
     * Get remaining requests in current minute
     */
    suspend fun getRemainingRequests(): Int = mutex.withLock {
        val now = Clock.System.now().toEpochMilliseconds()
        val oneMinuteAgo = now - 60_000
        requestTimestamps.removeAll { it < oneMinuteAgo }
        return maxRequestsPerMinute - requestTimestamps.size
    }
}
