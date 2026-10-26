package com.suman.ratelimitterplayground.storage

import com.suman.ratelimitterplayground.model.Window
import java.util.concurrent.ConcurrentHashMap

/*
Why: The algorithm should not care where data is stored. Today it’s an in-memory map. Tomorrow it could be RedisWindowStore, and you would add that one new class without touching the algorithm.
This is the Dependency Inversion Principle: depend on interfaces, not concrete classes.
 */
class InMemoryWindowStore : WindowStore {
    private val windows = ConcurrentHashMap<String, Window>()
    override fun compute(
        clientId: String,
        update: (Window?) -> Window
    ): Window {
        return windows.compute(clientId) { _, existing -> update(existing) }!!
    }

}