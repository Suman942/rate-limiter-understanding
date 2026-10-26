package com.suman.ratelimitterplayground.limiter

import android.util.Log
import com.suman.ratelimitterplayground.clock.Clock
import com.suman.ratelimitterplayground.model.RateLimitConfig
import com.suman.ratelimitterplayground.model.Window
import com.suman.ratelimitterplayground.storage.WindowStore

class FixedWindowRateLimiter(
    private val config: RateLimitConfig,
    private val store: WindowStore,
    private val clock: Clock
): RateLimiter {
    override fun allowRequest(clientId: String): Boolean {
        val now = clock.nowMillis()
        val windowStart = now - (now % config.windowSizeMillis)
        var allowed = false

        store.compute(clientId){existing->
            // 1. Start a fresh window if there is none, or the old one has expired
          val current =  if(existing == null || existing.start != windowStart) Window(start = windowStart,0)
            else existing

            // 2. Allow if under the limit, and count the request
            if(current.count < config.maxRequests){
                allowed = true
                current.copy(count = current.count + 1)
            }else{
                current
            }

        }
        
        return allowed
    }
}