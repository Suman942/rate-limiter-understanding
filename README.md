# rate-limiter-understanding
A thread-safe Fixed Window Rate Limiter implemented in Kotlin. Features configurable request limits, in-memory storage, and deterministic testing with an injectable clock. Explores the algorithm’s working, advantages, disadvantages, and boundary-burst behavior.


# Fixed Window Rate Limiter in Kotlin

https://play.kotlinlang.org/embed?short=tceE3dEyB&theme=darcula

A simple, beginner-friendly implementation of the Fixed Window Rate Limiting Algorithm using Kotlin.

This project explores how rate limiting works, how to structure the implementation using clean code principles, and how to test time-dependent behavior.

## 1. What Is Rate Limiting?

A rate limiter controls how many requests a client can make within a specified period.

For example, suppose an API allows each user to make a maximum of 3 requests per second.

- The first request is allowed.
- The second request is allowed.
- The third request is allowed.
- The fourth request is rejected.

The purpose is to prevent clients from sending too many requests within a given period.

## 2. What Is the Fixed Window Algorithm?

The Fixed Window Algorithm divides time into fixed-duration intervals called *windows*.

Each client has a request counter for the current window. When a request arrives, the rate limiter checks whether the client is still within the same window.

If the window has changed, the counter starts again from zero.

For this implementation, let's use the following configuration:

| Configuration | Value |
|---|---:|
| Maximum requests | 3 |
| Window duration | 1,000 milliseconds |
| Client ID | `user1` |

The client can make a maximum of 3 requests in each window.

## 3. How Does It Work?

The algorithm follows these steps:

1. Get the current timestamp.
2. Calculate the start of the current window.
3. Retrieve the client's previously stored window and request count.
4. If the client has no stored window or the window has changed, create a new window with a count of zero.
5. If the count is below the configured limit, allow the request and increment the count.
6. Otherwise, reject the request without incrementing the count.

### Request flow

```text
        Incoming Request
               |
               v
      Get Current Timestamp
               |
               v
      Calculate Window Start
               |
               v
       Read Client's State
               |
               v
       Has Window Changed?
          /          \
        Yes           No
         |             |
         v             v
   Create New       Keep Current
   Window           Window
   Count = 0        Count Unchanged
         \             /
          \           /
           v         v
       Is Count < Limit?
          /          \
        Yes           No
         |             |
         v             v
   Allow Request   Reject Request
   Increment Count Keep Count
```

## 4. Understanding the Window Calculation

The core formula is:

```kotlin
val now = clock.nowMillis()

val windowStart =
    now - (now % config.windowSizeMillis)
```

Here:

- `now` is the current timestamp in milliseconds.
- `windowSizeMillis` is the duration of each window.
- `%` calculates the remainder after division.
- `windowStart` identifies the beginning of the current window.

### Example

Suppose the current timestamp is `2500 ms` and the window duration is `1000 ms`.

First, calculate the remainder:

```text
2500 % 1000 = 500
```

Then subtract the remainder:

```text
2500 - 500 = 2000
```

Therefore, the current window starts at `2000 ms`.

```text
0             1000            2000            3000
|---------------|---------------|---------------|
    Window 0        Window 1        Window 2
                                    ^
                                 now = 2500
```

The current window covers timestamps from `2000 ms` up to, but not including, `3000 ms`.

Every timestamp within that interval produces the same `windowStart`.

**Important:** Windows are aligned to epoch-time boundaries. They do not start when a client sends its first request.

## 5. Example: Three Requests Per Window

Suppose the window starts at `0 ms` and the request limit is 3.

| Time | Window start | Count before request | Result | Count after request |
|---|---:|---:|---|---:|
| 100 ms | 0 ms | 0 | Allowed | 1 |
| 200 ms | 0 ms | 1 | Allowed | 2 |
| 500 ms | 0 ms | 2 | Allowed | 3 |
| 700 ms | 0 ms | 3 | Rejected | 3 |
| 999 ms | 0 ms | 3 | Rejected | 3 |
| 1,000 ms | 1,000 ms | 3 in old window | Allowed | 1 |
| 1,100 ms | 1,000 ms | 1 | Allowed | 2 |

At `1000 ms`, the window ID changes from `0` to `1000`. The next request initializes the new window and is allowed.

No background timer is needed. The counter resets when a request arrives and the limiter detects a new window.

## 6. Kotlin Implementation

The following class contains the core algorithm. It depends on `RateLimitConfig`, `Window`, `Clock`, and `WindowStore`, which are defined separately.

```kotlin
interface RateLimiter {
    fun allowRequest(clientId: String): Boolean
}

class FixedWindowRateLimiter(
    private val config: RateLimitConfig,
    private val store: WindowStore,
    private val clock: Clock
) : RateLimiter {

    override fun allowRequest(clientId: String): Boolean {
        val now = clock.nowMillis()

        val windowStart =
            now - (now % config.windowSizeMillis)

        var allowed = false

        store.compute(clientId) { existing ->

            val current =
                if (
                    existing == null ||
                    existing.start != windowStart
                ) {
                    Window(windowStart, 0)
                } else {
                    existing
                }

            if (current.count < config.maxRequests) {
                allowed = true
                current.copy(
                    count = current.count + 1
                )
            } else {
                current
            }
        }

        return allowed
    }
}
```

### Key components

- **`RateLimitConfig`** — holds the maximum request count and window duration.
- **`Window`** — stores the current window's start time and request count.
- **`Clock`** — provides the current timestamp.
- **`WindowStore`** — abstracts how each client's state is stored.
- **`FixedWindowRateLimiter`** — implements the allow-or-reject decision.

Separating these responsibilities makes the code easier to maintain and test.

If the store uses `ConcurrentHashMap.compute()`, updates for a particular client key can be performed atomically within that process. A multi-instance deployment would need shared state and appropriate atomic operations to enforce one global limit.

## 7. Testing the Window Boundary

Testing rate limiters using real time can make tests slow or unreliable.

To solve this, the implementation receives a `Clock` interface. We can supply a fake clock that returns a time we control.

```kotlin
class FakeClock(
    var time: Long = 0L
) : Clock {
    override fun nowMillis() = time
}
```

Now we can test the boundary without waiting:

```kotlin
val clock = FakeClock(time = 0L)

val limiter = FixedWindowRateLimiter(
    config = RateLimitConfig(
        maxRequests = 3,
        windowSizeMillis = 1000L
    ),
    store = InMemoryWindowStore(),
    clock = clock
)

repeat(3) {
    check(limiter.allowRequest("user1"))
}

check(!limiter.allowRequest("user1"))

clock.time = 999L
check(!limiter.allowRequest("user1"))

clock.time = 1000L
check(limiter.allowRequest("user1"))
```

The first three requests succeed. The fourth is rejected. At `999 ms`, the request is still rejected because the window has not changed. At `1000 ms`, the next window begins, so the request succeeds.

The fake clock makes the test deterministic because we control exactly when the boundary is crossed.

## 8. Advantages

### Simple implementation

The algorithm uses a timestamp, a window identifier, and a counter. It is easy to understand, implement, and debug.

### Low memory usage

Only the current window start and request count need to be stored for each client. The algorithm does not need to save every request timestamp.

### Fast processing

The algorithm performs a small number of calculations and a state lookup or update for each request.

### Easy configuration

The request limit and window duration can be configured independently. For example, an API could allow 100 requests per minute or 1,000 requests per hour.

### Easy testing

Injecting a clock makes it possible to test time boundaries without waiting for real time to pass.

### Straightforward maintenance

Interfaces for time and storage keep the algorithm independent of particular implementations, making it easier to modify and test.

## 9. Disadvantages

### Boundary burst problem

The biggest limitation is that a client can make requests near the end of one window and immediately make more requests when the next window begins.

For example, suppose the limit is 10 requests per minute.

```text
Window 1                         Window 2
12:00:00–12:00:59                12:01:00–12:01:59

                   12:00:59      12:01:01
                       |              |
                  10 requests     10 requests
                    Allowed         Allowed

                 20 requests in ~2 seconds
```

All 20 requests may be accepted even though the configured limit is 10 per window.

This happens because each fixed window has its own independent counter.

The algorithm correctly enforces the limit within each window, but it does not guarantee the same maximum over every rolling 60-second period.

### Traffic can arrive in bursts

A client can send all permitted requests at once. The algorithm limits the total per window, not how evenly requests are distributed throughout it.

### In-memory storage is local to one process

If multiple application instances use independent in-memory maps, each instance maintains its own counters. A client may exceed the intended global limit by sending requests to different instances.

### Client state requires lifecycle management

In-memory entries can accumulate as new client IDs appear. An implementation may need cleanup or expiration for inactive clients.

## 10. When Should You Use the Fixed Window Algorithm?

The Fixed Window Algorithm is suitable when:

- You need a simple rate limiter.
- Occasional bursts around window boundaries are acceptable.
- You want to enforce coarse quotas, such as requests per hour.
- Low memory usage is important.
- You want an easy-to-understand starting point for a rate-limiting implementation.

It may not be appropriate when the system requires strict rolling-period limits or more controlled traffic bursts.

## Conclusion

The Fixed Window Algorithm divides time into fixed intervals and maintains a request counter for each client.

Its main strengths are simplicity, low memory consumption, fast processing, and straightforward testing.

Its main limitation is the boundary burst problem: requests can accumulate at the end of one window and the beginning of the next.

Understanding how this algorithm works — including its strengths and limitations — gives us a solid foundation for learning rate limiting.

For now, the goal is to understand the Fixed Window Algorithm thoroughly. Once that's clear, we can explore the next algorithm and see which specific limitation it is designed to address.
