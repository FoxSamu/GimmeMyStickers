package net.foxboi.stickerbot.util

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A condition, similar to Java's `Condition`, allowing one to wait suspendingly.
 */
class Condition {
    private val continuations = mutableListOf<CancellableContinuation<Unit>>()

    suspend fun wait() {
        suspendCancellableCoroutine {
            synchronized(continuations) {
                continuations += it
            }
        }
    }

    fun signal() {
        synchronized(continuations) {
            for (cont in continuations) {
                cont.resume(Unit)
            }
            continuations.clear()
        }
    }

    fun cancel(cause: Throwable? = null) {
        synchronized(continuations) {
            for (cont in continuations) {
                cont.cancel(cause)
            }
            continuations.clear()
        }
    }
}