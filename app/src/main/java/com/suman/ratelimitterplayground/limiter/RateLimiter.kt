package com.suman.ratelimitterplayground.limiter

interface RateLimiter {
    fun allowRequest(clientId:String) : Boolean
}