package net.foxboi.gms.bot.flow

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SynchronizedCondition {
    private val continuations = mutableListOf<CancellableContinuation<Unit>>()
    private val lock = ReentrantLock()

    suspend fun wait() {
        suspendCancellableCoroutine {
            lock.withLock {
                continuations += it
            }
        }
    }

    fun signal() {
        lock.withLock {
            for (cont in continuations) {
                cont.resume(Unit)
            }
            continuations.clear()
        }
    }

    fun signalException(e: Throwable) {
        lock.withLock {
            for (cont in continuations) {
                cont.resumeWithException(e)
            }
            continuations.clear()
        }
    }

    fun cancel(cause: Throwable? = null) {
        lock.withLock {
            for (cont in continuations) {
                cont.cancel(cause)
            }
            continuations.clear()
        }
    }
}