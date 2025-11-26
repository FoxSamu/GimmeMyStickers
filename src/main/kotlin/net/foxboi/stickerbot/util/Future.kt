@file:Suppress("UNCHECKED_CAST")

package net.foxboi.stickerbot.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * A value that is to be set later, but that can already be waited on by coroutines.
 */
class Future<T> {
    private val condition = Condition()
    private var set = false
    private var value: T? = null
    private var exception: Throwable? = null
    private var cancelled = false

    private val mutex = Mutex()

    private fun checkDoubleSet() {
        if (set) {
            throw IllegalStateException("Double set")
        }
    }

    suspend fun set(v: T) = mutex.withLock {
        checkDoubleSet()

        value = v
        set = true
        condition.signal()
    }

    suspend fun setException(e: Throwable) = mutex.withLock {
        exception = e
        set = true
        condition.signalException(e)
    }

    suspend fun setCancelled(cause: Throwable? = null) = mutex.withLock {
        exception = CancellationException(cause)
        cancelled = true
        set = true
        condition.cancel()
    }

    suspend fun get(): T = mutex.withLock {
        while (!set) {
            mutex.withUnlock {
                condition.wait()
            }
        }

        val e = exception
        if (e != null) {
            throw e
        } else {
            return value as T
        }
    }

    suspend fun isSet(): Boolean {
        return mutex.withLock { set }
    }

    suspend fun isCancelled(): Boolean {
        return mutex.withLock { cancelled }
    }

    suspend fun tryGet(): T? {
        return mutex.withLock { value }
    }

    suspend fun tryGetException(): Throwable? {
        return mutex.withLock { exception }
    }
}

suspend fun Future<Unit>.setDone() {
    set(Unit)
}