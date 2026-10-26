package com.suman.ratelimitterplayground

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.suman.ratelimitterplayground.clock.SystemClock
import com.suman.ratelimitterplayground.handler.RequestHandler
import com.suman.ratelimitterplayground.limiter.FixedWindowRateLimiter
import com.suman.ratelimitterplayground.model.RateLimitConfig
import com.suman.ratelimitterplayground.storage.InMemoryWindowStore
import com.suman.ratelimitterplayground.ui.theme.RateLimitterPlaygroundTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RateLimitterPlaygroundTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                  val config = RateLimitConfig(1,1000)
                    val store = InMemoryWindowStore()
                    val limiter = FixedWindowRateLimiter(config,store, SystemClock)
                    val handler = RequestHandler(limiter)

                    repeat(5){
                        i -> Log.d("RateLimiter","Request ${i+1} : ${handler.handle("user1")}")
                    }

                    Thread.sleep(800)
                    Log.d("RateLimiter","After reset: ${handler.handle("user1")}")

                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RateLimitterPlaygroundTheme {
        Greeting("Android")
    }
}