package com.suman.ratelimitterplayground.clock

/*
Why: If the algorithm called System.currentTimeMillis() directly, you could never test “what happens after 1 second” without really waiting.
With an interface, a test can pass in a fake clock.
 */
object SystemClock : Clock {
    override fun nowMillis(): Long {
        return System.currentTimeMillis()
    }
}