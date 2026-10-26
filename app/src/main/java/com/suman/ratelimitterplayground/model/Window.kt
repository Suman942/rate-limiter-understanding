package com.suman.ratelimitterplayground.model

/*
Why: Models only hold data. Window is immutable (val), so to change it we create a new copy.
Immutable objects are much safer when many threads are involved.
 */
data class Window(
    val start: Long, // when this window began
    val count: Int   // request made in this window
)
