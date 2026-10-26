package com.suman.ratelimitterplayground.handler

import com.suman.ratelimitterplayground.limiter.RateLimiter

/*
Why: This is the layer that talks to the outside world, like a controller in a web app. It depends on the RateLimiter interface, so it would work unchanged with a token bucket limiter.
 */
class RequestHandler(private val rateLimiter: RateLimiter) {
    
    fun handle(clientId: String): String{
       return if(rateLimiter.allowRequest(clientId)) "OK"
        else "429 too many request"
    }
}