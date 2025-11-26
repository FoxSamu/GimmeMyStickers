package net.foxboi.stickerbot.util

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A condition, similar to Java's `Condition`, allowing one to wait suspendingly.
 *
 */
class Condition {
    private val continuations = mutableListOf<CancellableContinuation<Unit>>()
    private val mutex = Mutex()

    suspend fun wait() {
        mutex.lock()
        try {
            suspendCancellableCoroutine {
                try {
                    continuations += it
                } finally {
                    mutex.unlock()
                }
            }
        } finally {
            if (mutex.isLocked) {
                mutex.unlock()
            }
        }
    }

    suspend fun signal() {
        val conts = mutex.withLock {
            val conts = continuations.toList()
            continuations.clear()
            conts
        }
        for (cont in conts) {
            cont.resume(Unit)
        }
    }

    suspend fun signalException(e: Throwable) {
        val conts = mutex.withLock {
            val conts = continuations.toList()
            continuations.clear()
            conts
        }
        for (cont in conts) {
            cont.resumeWithException(e)
        }
    }

    suspend fun cancel(cause: Throwable? = null) {
        val conts = mutex.withLock {
            val conts = continuations.toList()
            continuations.clear()
            conts
        }
        for (cont in conts) {
            cont.cancel(cause)
        }
    }
}