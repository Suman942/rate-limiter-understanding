package com.suman.ratelimitterplayground.model

data class RateLimitConfig(
    val maxRequests: Int,
    val windowSizeMillis : Long
){
    init {
        require(maxRequests > 0) { "maxRequests must be positive" }
        require(windowSizeMillis > 0) { "windowSizeMillis must be positive" }
    }
}
