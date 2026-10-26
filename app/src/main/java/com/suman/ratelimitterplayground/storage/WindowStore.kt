package com.suman.ratelimitterplayground.storage

import com.suman.ratelimitterplayground.model.Window

interface WindowStore {
    /** Atomically read the client's window, transform it, and save the result. */
    fun compute(clientId: String,update:(Window?)-> Window) : Window
}