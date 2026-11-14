package cc.kesardev.scparchive.data

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes wiki requests and keeps at least 1.5 s between them.
 * Shared by article fetches and catalog fetches.
 */
object RequestRateLimiter {

    private val mutex = Mutex()
    private var lastRequestAt = 0L

    suspend fun await(minIntervalMs: Long = 1500L) {
        mutex.withLock {
            val now = SystemClock.elapsedRealtime()
            val wait = minIntervalMs - (now - lastRequestAt)
            if (wait > 0) {
                delay(wait)
            }
            lastRequestAt = SystemClock.elapsedRealtime()
        }
    }
}
